package dev.ftb.mods.ftbquests.quest;

import com.mojang.util.UUIDTypeAdapter;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.events.QuestProgressEventData;
import dev.ftb.mods.ftbquests.net.*;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import dev.ftb.mods.ftbquests.util.FileUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.event.TeamCreatedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

public class ServerQuestFile extends QuestFile {
	public static final LevelResource FTBQUESTS_DATA = new LevelResource("ftbquests");

	public static ServerQuestFile INSTANCE;

	public final MinecraftServer server;
	private boolean shouldSave;
	private boolean isLoading;
	private Path folder;
	private ServerPlayer currentPlayer = null;

	public ServerQuestFile(MinecraftServer s) {
		server = s;
		shouldSave = false;
		isLoading = false;

		int taskTypeId = 0;

		for (TaskType type : TaskTypes.TYPES.values()) {
			type.intId = ++taskTypeId;
			taskTypeIds.put(type.intId, type);
		}

		int rewardTypeId = 0;

		for (RewardType type : RewardTypes.TYPES.values()) {
			type.intId = ++rewardTypeId;
			rewardTypeIds.put(type.intId, type);
		}
	}

	public void load() {
		folder = Platform.getConfigFolder().resolve("ftbquests/quests");

		if (Files.exists(folder)) {
			FTBQuests.LOGGER.info("Loading quests from " + folder);
			isLoading = true;
			readDataFull(folder);
			isLoading = false;
		}

		Path path = server.getWorldPath(FTBQUESTS_DATA);

		if (Files.exists(path)) {
			try (Stream<Path> s = Files.list(path)) {
				s.filter(p -> p.getFileName().toString().contains("-") && p.getFileName().toString().endsWith(".snbt")).forEach(path1 -> {
					SNBTCompoundTag nbt = SNBT.read(path1);

					if (nbt != null) {
						try {
							UUID uuid = UUIDTypeAdapter.fromString(nbt.getString("uuid"));
							TeamData data = new TeamData(uuid);
							data.file = this;
							addData(data, true);
							data.deserializeNBT(nbt);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public Env getSide() {
		return Env.SERVER;
	}

	@Override
	public boolean isLoading() {
		return isLoading;
	}

	@Override
	public Path getFolder() {
		return folder;
	}

	@Override
	public void deleteObject(long id) {
		QuestObjectBase object = getBase(id);

		if (object != null) {
			String file = object.getPath();

			object.deleteChildren();
			object.deleteSelf();
			refreshIDMap();
			save();

			if (file != null) {
				FileUtils.delete(getFolder().resolve(file).toFile());
			}
		}

		new DeleteObjectResponseMessage(id).sendToAll(server);
	}

	@Override
	public void save() {
		shouldSave = true;
	}

	public void saveNow() {
		if (shouldSave) {
			writeDataFull(getFolder());
			shouldSave = false;
		}

		getAllData().forEach(TeamData::saveIfChanged);
	}

	public void unload() {
		saveNow();
		deleteChildren();
		deleteSelf();
	}

	public ServerPlayer getCurrentPlayer() {
		return currentPlayer;
	}

	public void withPlayerContext(ServerPlayer player, Runnable toDo) {
		currentPlayer = player;
		try {
			toDo.run();
		} finally {
			currentPlayer = null;
		}
	}

	public void playerLoggedIn(PlayerLoggedInAfterTeamEvent event) {
		ServerPlayer player = event.getPlayer();
		TeamData data = getData(event.getTeam());

		new SyncQuestsMessage(this).sendTo(player);

		for (TeamData teamData : teamDataMap.values()) {
			new SyncTeamDataMessage(teamData, teamData == data).sendTo(player);
		}

		player.inventoryMenu.addSlotListener(new FTBQuestsInventoryListener(player));

		if (!data.isLocked()) {
			withPlayerContext(player, () -> {
				for (ChapterGroup group : chapterGroups) {
					for (Chapter chapter : group.chapters) {
						for (Quest quest : chapter.getQuests()) {
							if (!data.isCompleted(quest) && quest.isCompletedRaw(data)) {
								// Handles possible situation where quest book has been modified to remove a task from a quest
								// It can leave a player having completed all the other tasks, but unable to complete the quest
								//   since quests are normally marked completed when the last task in that quest is completed
								// https://github.com/FTBBeta/Beta-Testing-Issues/issues/755
								quest.onCompleted(new QuestProgressEventData<>(new Date(), data, quest, data.getOnlineMembers(), Collections.singletonList(player)));
							}

							data.checkAutoCompletion(quest);

							if (data.canStartTasks(quest)) {
								for (Task task : quest.tasks) {
									if (task.checkOnLogin()) {
										task.submitTask(data, player);
									}
								}
							}
						}
					}
				}
			});
		}
	}

	public void teamCreated(TeamCreatedEvent event) {
		UUID id = event.getTeam().getId();
		TeamData data = teamDataMap.get(id);

		if (data == null) {
			data = new TeamData(id);
			data.file = this;
			data.markDirty();
		}

		String displayName = event.getTeam().getDisplayName();

		if (!data.name.equals(displayName)) {
			data.name = displayName;
			data.markDirty();
		}

		addData(data, false);

		if (event.getTeam() instanceof PartyTeam) {
			PlayerTeam pt = event.getTeam().manager.getInternalPlayerTeam(event.getCreator().getUUID());
			TeamData oldTeamData = getData(pt);
			data.copyData(oldTeamData);
		}

		TeamDataUpdate self = new TeamDataUpdate(data);

		new CreateOtherTeamDataMessage(self).sendToAll(server);
	}

	public void playerChangedTeam(PlayerChangedTeamEvent event) {
		if (event.getPreviousTeam().isPresent()) {
			TeamData oldTeamData = getData(event.getPreviousTeam().get());
			TeamData newTeamData = getData(event.getTeam());

			if (event.getPreviousTeam().get() instanceof PlayerTeam && event.getTeam() instanceof PartyTeam && !((PartyTeam) event.getTeam()).isOwner(event.getPlayerId())) {
				newTeamData.mergeData(oldTeamData);
			}

			new TeamDataChangedMessage(new TeamDataUpdate(oldTeamData), new TeamDataUpdate(newTeamData)).sendToAll(server);
			new SyncTeamDataMessage(newTeamData, true).sendTo(event.getTeam().getOnlineMembers());
		}
	}

	@Override
	public boolean isPlayerOnTeam(Player player, TeamData teamData) {
		return FTBTeamsAPI.getPlayerTeamID(player.getUUID()).equals(teamData.uuid);
	}
}
