package dev.ftb.mods.ftbquests.quest.task;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.util.KnownServerRegistries;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class DimensionTask extends BooleanTask {
	public ResourceKey<Level> dimension;

	public DimensionTask(Quest quest) {
		super(quest);
		dimension = Level.NETHER;
	}

	@Override
	public TaskType getType() {
		return TaskTypes.DIMENSION;
	}

	@Override
	public void writeData(CompoundTag nbt) {
		super.writeData(nbt);
		nbt.putString("dimension", dimension.location().toString());
	}

	@Override
	public void readData(CompoundTag nbt) {
		super.readData(nbt);
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString("dimension")));
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeResourceLocation(dimension.location());
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buffer.readResourceLocation());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);

		if (KnownServerRegistries.client != null && !KnownServerRegistries.client.dimensions.isEmpty()) {
			config.addEnum("dim", dimension.location(), v -> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, v), NameMap.of(KnownServerRegistries.client.dimensions.iterator().next(), KnownServerRegistries.client.dimensions.toArray(new ResourceLocation[0])).create());
		} else {
			config.addString("dim", dimension.location().toString(), v -> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(v)), "minecraft:the_nether");
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public MutableComponent getAltTitle() {
		return Component.translatable("ftbquests.task.ftbquests.dimension").append(": ").append(Component.literal(dimension.location().toString()).withStyle(ChatFormatting.DARK_GREEN));
	}

	@Override
	public int autoSubmitOnPlayerTick() {
		return 100;
	}

	@Override
	public boolean canSubmit(TeamData teamData, ServerPlayer player) {
		return !player.isSpectator() && player.level.dimension() == dimension;
	}
}
