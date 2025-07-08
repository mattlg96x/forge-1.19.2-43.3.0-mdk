package dev.ftb.mods.ftbquests.quest;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.events.ObjectStartedEvent;
import dev.ftb.mods.ftbquests.events.QuestProgressEventData;
import dev.ftb.mods.ftbquests.net.DisplayCompletionToastMessage;
import dev.ftb.mods.ftbquests.util.NetUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.stream.Collectors;

public final class Chapter extends QuestObject {
	public final QuestFile file;
	public ChapterGroup group;
	public String filename;
	private final List<Quest> quests;
	private final List<QuestLink> questLinks;
	public final List<String> subtitle;
	public boolean alwaysInvisible;
	public String defaultQuestShape;
	public final List<ChapterImage> images;
	public boolean defaultHideDependencyLines;
	public int defaultMinWidth = 0;
	private ProgressionMode progressionMode;
	private boolean hideQuestDetailsUntilStartable;
	private boolean hideQuestUntilDepsVisible;

	public Chapter(QuestFile f, ChapterGroup g) {
		file = f;
		group = g;
		filename = "";
		quests = new ArrayList<>();
		questLinks = new ArrayList<>();
		subtitle = new ArrayList<>(0);
		alwaysInvisible = false;
		defaultQuestShape = "";
		images = new ArrayList<>();
		defaultHideDependencyLines = false;
		progressionMode = ProgressionMode.DEFAULT;
		hideQuestUntilDepsVisible = false;
		hideQuestDetailsUntilStartable = false;
	}

	@Override
	public QuestObjectType getObjectType() {
		return QuestObjectType.CHAPTER;
	}

	@Override
	public QuestFile getQuestFile() {
		return group.file;
	}

	@Override
	public Chapter getQuestChapter() {
		return this;
	}

	@Override
	public void writeData(CompoundTag nbt) {
		nbt.putString("filename", filename);
		super.writeData(nbt);

		if (!subtitle.isEmpty()) {
			ListTag list = new ListTag();

			for (String v : subtitle) {
				list.add(StringTag.valueOf(v));
			}

			nbt.put("subtitle", list);
		}

		if (alwaysInvisible) {
			nbt.putBoolean("always_invisible", true);
		}

		nbt.putString("default_quest_shape", defaultQuestShape);
		nbt.putBoolean("default_hide_dependency_lines", defaultHideDependencyLines);

		if (!images.isEmpty()) {
			ListTag list = new ListTag();

			for (ChapterImage image : images) {
				SNBTCompoundTag nbt1 = new SNBTCompoundTag();
				image.writeData(nbt1);
				list.add(nbt1);
			}

			nbt.put("images", list);
		}

		if (defaultMinWidth > 0) {
			nbt.putInt("default_min_width", defaultMinWidth);
		}

		if (progressionMode != ProgressionMode.DEFAULT) {
			nbt.putString("progression_mode", progressionMode.getId());
		}

		if (hideQuestDetailsUntilStartable) nbt.putBoolean("hide_quest_details_until_startable", true);
		if (hideQuestUntilDepsVisible) nbt.putBoolean("hide_quest_until_deps_visible", true);
	}

	@Override
	public void readData(CompoundTag nbt) {
		filename = nbt.getString("filename");
		super.readData(nbt);
		subtitle.clear();

		ListTag subtitleNBT = nbt.getList("subtitle", Tag.TAG_STRING);

		for (int i = 0; i < subtitleNBT.size(); i++) {
			subtitle.add(subtitleNBT.getString(i));
		}

		alwaysInvisible = nbt.getBoolean("always_invisible");
		defaultQuestShape = nbt.getString("default_quest_shape");

		if (defaultQuestShape.equals("default")) {
			defaultQuestShape = "";
		}

		defaultHideDependencyLines = nbt.getBoolean("default_hide_dependency_lines");

		ListTag imgs = nbt.getList("images", Tag.TAG_COMPOUND);

		images.clear();

		for (int i = 0; i < imgs.size(); i++) {
			ChapterImage image = new ChapterImage(this);
			image.readData(imgs.getCompound(i));
			images.add(image);
		}

		defaultMinWidth = nbt.getInt("default_min_width");
		progressionMode = ProgressionMode.NAME_MAP.get(nbt.getString("progression_mode"));
		hideQuestDetailsUntilStartable = nbt.getBoolean("hide_quest_details_until_startable");
		hideQuestUntilDepsVisible = nbt.getBoolean("hide_quest_until_deps_visible");
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeUtf(filename, Short.MAX_VALUE);
		NetUtils.writeStrings(buffer, subtitle);
		buffer.writeBoolean(alwaysInvisible);
		buffer.writeUtf(defaultQuestShape, Short.MAX_VALUE);
		NetUtils.write(buffer, images, (d, img) -> img.writeNetData(d));
		buffer.writeBoolean(defaultHideDependencyLines);
		buffer.writeInt(defaultMinWidth);
		ProgressionMode.NAME_MAP.write(buffer, progressionMode);
		buffer.writeBoolean(hideQuestDetailsUntilStartable);
		buffer.writeBoolean(hideQuestUntilDepsVisible);
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		filename = buffer.readUtf(Short.MAX_VALUE);
		NetUtils.readStrings(buffer, subtitle);
		alwaysInvisible = buffer.readBoolean();
		defaultQuestShape = buffer.readUtf(Short.MAX_VALUE);
		NetUtils.read(buffer, images, d -> {
			ChapterImage image = new ChapterImage(this);
			image.readNetData(d);
			return image;
		});
		defaultHideDependencyLines = buffer.readBoolean();
		defaultMinWidth = buffer.readInt();
		progressionMode = ProgressionMode.NAME_MAP.read(buffer);
		hideQuestDetailsUntilStartable = buffer.readBoolean();
		hideQuestUntilDepsVisible = buffer.readBoolean();
	}

	public int getIndex() {
		return group.chapters.indexOf(this);
	}

	@Override
	public int getRelativeProgressFromChildren(TeamData data) {
		if (alwaysInvisible) {
			return 100;
		}

		if (quests.isEmpty()) {
			return 100;
		}

		int progress = 0;
		int count = 0;

		for (Quest quest : quests) {
			if (!quest.isProgressionIgnored()) {
				progress += data.getRelativeProgress(quest);
				count++;
			}
		}

		if (count <= 0) {
			return 100;
		}

		return getRelativeProgressFromChildren(progress, count);
	}

	@Override
	public void onStarted(QuestProgressEventData<?> data) {
		data.teamData.setStarted(id, data.time);
		ObjectStartedEvent.CHAPTER.invoker().act(new ObjectStartedEvent.ChapterEvent(data.withObject(this)));

		if (!data.teamData.isStarted(file)) {
			file.onStarted(data.withObject(file));
		}
	}

	@Override
	public void onCompleted(QuestProgressEventData<?> data) {
		data.teamData.setCompleted(id, data.time);
		ObjectCompletedEvent.CHAPTER.invoker().act(new ObjectCompletedEvent.ChapterEvent(data.withObject(this)));

		if (!disableToast) {
			for (ServerPlayer player : data.notifiedPlayers) {
				new DisplayCompletionToastMessage(id).sendTo(player);
			}
		}

		for (ChapterGroup g : file.chapterGroups) {
			for (Chapter chapter : g.chapters) {
				for (Quest quest : chapter.quests) {
					if (quest.hasDependency(this)) {
						data.teamData.checkAutoCompletion(quest);
					}
				}
			}
		}

		if (group.isCompletedRaw(data.teamData)) {
			group.onCompleted(data.withObject(group));
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public MutableComponent getAltTitle() {
		return Component.translatable("ftbquests.unnamed");
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Icon getAltIcon() {
		List<Icon> list = new ArrayList<>();

		for (Quest quest : quests) {
			list.add(quest.getIcon());
		}

		return IconAnimation.fromList(list, false);
	}

	@Override
	public void deleteSelf() {
		super.deleteSelf();
		group.chapters.remove(this);
	}

	@Override
	public void deleteChildren() {
		for (Quest quest : quests) {
			quest.deleteChildren();
			quest.invalid = true;
		}

		quests.clear();
	}

	@Override
	public void onCreated() {
		if (filename.isEmpty()) {
			String s = titleToID(title).orElse(toString());
			filename = s;

			Set<String> existingNames = group.file.chapterGroups.stream()
					.flatMap(g -> g.chapters.stream())
					.map(ch -> ch.filename)
					.collect(Collectors.toSet());

			for (int i = 2; existingNames.contains(filename); i++) {
				filename = s + "_" + i;
			}
		}

		group.chapters.add(this);

		if (!quests.isEmpty()) {
			List<Quest> l = new ArrayList<>(quests);
			quests.clear();
			for (Quest quest : l) {
				quest.onCreated();
			}
		}
	}

	public String getFilename() {
		if (filename.isEmpty()) {
			filename = getCodeString(this);
		}

		return filename;
	}

	@Override
	public String getPath() {
		return "chapters/" + getFilename() + ".snbt";
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);
		config.addList("subtitle", subtitle, new StringConfig(null), "");
		config.addBool("always_invisible", alwaysInvisible, v -> alwaysInvisible = v, false);
		config.addEnum("default_quest_shape", defaultQuestShape.isEmpty() ? "default" : defaultQuestShape, v -> defaultQuestShape = v.equals("default") ? "" : v, QuestShape.idMapWithDefault);
		config.addBool("default_hide_dependency_lines", defaultHideDependencyLines, v -> defaultHideDependencyLines = v, false);
		config.addInt("default_min_width", defaultMinWidth, v -> defaultMinWidth = v, 0, 0, 3000);
		config.addEnum("progression_mode", progressionMode, v -> progressionMode = v, ProgressionMode.NAME_MAP);
		config.addBool("hide_quest_details_until_startable", hideQuestDetailsUntilStartable, v -> hideQuestDetailsUntilStartable = v, false);
		config.addBool("hide_quest_until_deps_visible", hideQuestUntilDepsVisible, v -> hideQuestUntilDepsVisible = v, false);
	}

	@Override
	public boolean isVisible(TeamData data) {
		if (alwaysInvisible) {
			return false;
		}

		return hasVisibleQuest(data);
	}

	@Override
	public void clearCachedData() {
		super.clearCachedData();

		for (Quest quest : quests) {
			quest.clearCachedData();
		}
	}

	@Override
	protected void verifyDependenciesInternal(long original, int depth) {
		if (depth >= 1000) {
			throw new DependencyDepthException(this);
		}

		for (Quest quest : quests) {
			if (quest.id == original) {
				throw new DependencyLoopException(this);
			}

			quest.verifyDependenciesInternal(original, depth + 1);
		}
	}

	public boolean hasGroup() {
		return !group.isDefaultGroup();
	}

	public String getDefaultQuestShape() {
		return defaultQuestShape.isEmpty() ? file.getDefaultQuestShape() : defaultQuestShape;
	}

	@Override
	public Collection<? extends QuestObject> getChildren() {
		return quests;
	}

	@Override
	public boolean hasUnclaimedRewardsRaw(TeamData teamData, UUID player) {
		for (Quest quest : quests) {
			if (teamData.hasUnclaimedRewards(player, quest)) {
				return true;
			}
		}

		return false;
	}

	public ProgressionMode getProgressionMode() {
		return progressionMode == ProgressionMode.DEFAULT ? file.getProgressionMode() : progressionMode;
	}

	public boolean hideQuestDetailsUntilStartable() {
		return hideQuestDetailsUntilStartable;
	}

	public boolean hideQuestUntilDepsVisible() {
		return hideQuestUntilDepsVisible;
	}

	public boolean hasVisibleQuestLazy() {
		return !quests.isEmpty() || !questLinks.isEmpty();
	}

	public boolean hasVisibleQuest(TeamData data) {
		for (QuestObject object : getAllQuests()) {
			if (object.isVisible(data)) {
				return true;
			}
		}

		return false;
	}

	public List<QuestObject> getAllQuests() {
		List<QuestObject> list = new ArrayList<>();
		list.addAll(quests);
		list.addAll(questLinks);
		return list;
	}

	public List<Quest> getQuests() {
		return quests;
	}

	public List<QuestLink> getQuestLinks() {
		return questLinks;
	}
}
