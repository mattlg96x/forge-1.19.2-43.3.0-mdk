package dev.ftb.mods.ftbquests.quest.reward;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbquests.net.DisplayRewardToastMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class XPReward extends Reward {
	public int xp;

	public XPReward(Quest quest, int x) {
		super(quest);
		xp = x;
	}

	public XPReward(Quest quest) {
		this(quest, 100);
	}

	@Override
	public RewardType getType() {
		return RewardTypes.XP;
	}

	@Override
	public void writeData(CompoundTag nbt) {
		super.writeData(nbt);
		nbt.putInt("xp", xp);
	}

	@Override
	public void readData(CompoundTag nbt) {
		super.readData(nbt);
		xp = nbt.getInt("xp");
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeVarInt(xp);
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		xp = buffer.readVarInt();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);
		config.addInt("xp", xp, v -> xp = v, 100, 1, Integer.MAX_VALUE).setNameKey("ftbquests.reward.ftbquests.xp");
	}

	@Override
	public void claim(ServerPlayer player, boolean notify) {
		player.giveExperiencePoints(xp);

		if (notify) {
			new DisplayRewardToastMessage(id, Component.translatable("ftbquests.reward.ftbquests.xp").append(": ").append(Component.literal("+" + xp).withStyle(ChatFormatting.GREEN)), Color4I.EMPTY).sendTo(player);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public MutableComponent getAltTitle() {
		return Component.translatable("ftbquests.reward.ftbquests.xp").append(": ").append(Component.literal("+" + xp).withStyle(ChatFormatting.GREEN));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public String getButtonText() {
		return "+" + xp;
	}
}
