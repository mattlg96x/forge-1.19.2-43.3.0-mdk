package dev.ftb.mods.ftbquests.quest.task;

import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public abstract class BooleanTask extends Task {
	public BooleanTask(Quest q) {
		super(q);
	}

	@Override
	public String formatMaxProgress() {
		return "1";
	}

	@Override
	public String formatProgress(TeamData teamData, long progress) {
		return progress >= 1L ? "1" : "0";
	}

	public abstract boolean canSubmit(TeamData teamData, ServerPlayer player);

	@Override
	public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
		if (!teamData.isCompleted(this) && canSubmit(teamData, player)) {
			teamData.setProgress(this, 1L);
		}
	}
}
