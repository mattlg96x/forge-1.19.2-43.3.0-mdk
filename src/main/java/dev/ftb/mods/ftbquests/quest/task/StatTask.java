package dev.ftb.mods.ftbquests.quest.task;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class StatTask extends Task {
	public ResourceLocation stat;
	public int value = 1;

	public StatTask(Quest quest) {
		super(quest);
		stat = Stats.MOB_KILLS;
	}

	@Override
	public TaskType getType() {
		return TaskTypes.STAT;
	}

	@Override
	public long getMaxProgress() {
		return value;
	}

	@Override
	public String formatMaxProgress() {
		return Integer.toString(value);
	}

	@Override
	public String formatProgress(TeamData teamData, long progress) {
		return Long.toUnsignedString(progress);
	}

	@Override
	public void writeData(CompoundTag nbt) {
		super.writeData(nbt);
		nbt.putString("stat", stat.toString());
		nbt.putInt("value", value);
	}

	@Override
	public void readData(CompoundTag nbt) {
		super.readData(nbt);
		stat = new ResourceLocation(nbt.getString("stat"));
		value = nbt.getInt("value");
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeResourceLocation(stat);
		buffer.writeVarInt(value);
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		stat = buffer.readResourceLocation();
		value = buffer.readVarInt();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);

		List<ResourceLocation> list = new ArrayList<>();
		Stats.CUSTOM.iterator().forEachRemaining(s -> list.add(s.getValue()));
		config.addEnum("stat", stat, v -> stat = v, NameMap.of(Stats.MOB_KILLS, list).name(v -> Component.translatable("stat." + v.getNamespace() + "." + v.getPath())).create());
		config.addInt("value", value, v -> value = v, 1, 1, Integer.MAX_VALUE);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public MutableComponent getAltTitle() {
		return Component.translatable("stat." + stat.getNamespace() + "." + stat.getPath());
	}

	@Override
	public int autoSubmitOnPlayerTick() {
		return 3;
	}

	@Override
	public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
		if (teamData.isCompleted(this)) {
			return;
		}

		ResourceLocation statId = Registry.CUSTOM_STAT.get(stat);

		// workaround for a bug where mods might register a modded stat in the vanilla namespace
		//  https://github.com/FTBTeam/FTB-Mods-Issues/issues/724
		if (statId == null) statId = Registry.CUSTOM_STAT.get(new ResourceLocation(stat.getPath()));

		if (statId != null) {
			// could be null, if someone brought an FTB Quests save from a different world and the stat's missing here
			int set = Math.min(value, player.getStats().getValue(Stats.CUSTOM.get(statId)));
			if (set > teamData.getProgress(this)) {
				teamData.setProgress(this, set);
			}
		}
	}
}
