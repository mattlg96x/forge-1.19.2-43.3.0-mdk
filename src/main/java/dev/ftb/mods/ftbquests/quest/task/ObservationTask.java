package dev.ftb.mods.ftbquests.quest.task;

import com.mojang.brigadier.StringReader;
import dev.architectury.registry.registries.Registries;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public class ObservationTask extends BooleanTask {
	public long timer;
	public ObserveType observeType;
	public String toObserve;

	public ObservationTask(Quest quest) {
		super(quest);
		timer = 0L;
		observeType = ObserveType.BLOCK;
		toObserve = "minecraft:dirt";
	}

	@Override
	public TaskType getType() {
		return TaskTypes.OBSERVATION;
	}

	@Override
	public void writeData(CompoundTag nbt) {
		super.writeData(nbt);
		nbt.putLong("timer", timer);
		nbt.putInt("observe_type", observeType.ordinal());
		nbt.putString("to_observe", toObserve);
	}

	@Override
	public void readData(CompoundTag nbt) {
		super.readData(nbt);
		timer = nbt.getLong("timer");
		observeType = ObserveType.values()[nbt.getInt("observe_type")];
		toObserve = nbt.getString("to_observe");
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeVarLong(timer);
		buffer.writeEnum(observeType);
		buffer.writeUtf(toObserve);
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		timer = buffer.readVarLong();
		observeType = buffer.readEnum(ObserveType.class);
		toObserve = buffer.readUtf(Short.MAX_VALUE);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);
		config.addLong("timer", timer, v -> timer = v, 0L, 0L, 1200L);
		config.addEnum("observe_type", observeType, v -> observeType = v, ObserveType.NAME_MAP);
		config.addString("to_observe", toObserve, v -> toObserve = v, "minecraft:dirt");
	}

	@Override
	public Component getAltTitle() {
		return Component.translatable("ftbquests.task.ftbquests.observation").append(": ")
				.append(Component.literal(toObserve).withStyle(ChatFormatting.DARK_GREEN));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onButtonClicked(Button button, boolean canClick) {
	}

	@Override
	public boolean canSubmit(TeamData teamData, ServerPlayer player) {
		return true;
	}

	@Override
	public boolean checkOnLogin() {
		return false;
	}

	public boolean observe(Player player, HitResult result) {
		if (toObserve.isEmpty()) {
			return false;
		}

		if (result instanceof BlockHitResult blockResult) {
			BlockInWorld blockInWorld = new BlockInWorld(player.level, blockResult.getBlockPos(), false);

			BlockState state = blockInWorld.getState();
			//noinspection ConstantValue
			if (state == null) {
				// shouldn't normally occur, but potential race condition when switching dimensions?
				return false;
			}
			Block block = state.getBlock();
			BlockEntity blockEntity = blockInWorld.getEntity();

			switch (observeType) {
				case BLOCK -> {
					return String.valueOf(Registries.getId(block, Registry.BLOCK_REGISTRY)).equals(toObserve);
				}
				case BLOCK_TAG -> {
					return asTagRL(toObserve)
							.map(rl -> state.is(TagKey.create(Registry.BLOCK_REGISTRY, rl)))
							.orElse(false);
				}
				case BLOCK_STATE -> {
					BlockInput stateMatch = tryMatchBlock(toObserve, false);
					return stateMatch != null && stateMatch.test(blockInWorld);
				}
				case BLOCK_ENTITY -> {
					BlockInput stateNbtMatch = tryMatchBlock(toObserve, true);
					return stateNbtMatch != null && stateNbtMatch.test(blockInWorld);
				}
				case BLOCK_ENTITY_TYPE -> {
					return blockEntity != null && toObserve.equals(String.valueOf(Registries.getId(blockEntity.getType(), Registry.BLOCK_ENTITY_TYPE_REGISTRY)));
				}
				default -> {
					return false;
				}
			}
		} else if (result instanceof EntityHitResult entityResult) {
			if (observeType == ObserveType.ENTITY_TYPE) {
				return toObserve.equals(String.valueOf(Registries.getId(entityResult.getEntity().getType(), Registry.ENTITY_TYPE_REGISTRY)));
			} else if (observeType == ObserveType.ENTITY_TYPE_TAG) {
				return asTagRL(toObserve)
						.map(rl -> entityResult.getEntity().getType().is(TagKey.create(Registry.ENTITY_TYPE_REGISTRY, rl)))
						.orElse(false);
			}
		}

		return false;
	}

	private Optional<ResourceLocation> asTagRL(String str) {
		try {
			return Optional.of(new ResourceLocation(str.startsWith("#") ? str.substring(1) : str));
		} catch (ResourceLocationException e) {
			return Optional.empty();
		}
	}

	private BlockInput tryMatchBlock(String string, boolean parseNbt) {
		try {
			BlockStateParser.BlockResult blockStateParser = BlockStateParser.parseForBlock(Registry.BLOCK, new StringReader(string), false);
			return new BlockInput(blockStateParser.blockState(), blockStateParser.properties().keySet(), parseNbt ? blockStateParser.nbt() : null);
		} catch (Exception ex) {
			return null;
		}
	}

	enum ObserveType {
		BLOCK,
		BLOCK_TAG,
		BLOCK_STATE,
		BLOCK_ENTITY,
		BLOCK_ENTITY_TYPE,
		ENTITY_TYPE,
		ENTITY_TYPE_TAG;

		public static final NameMap<ObserveType> NAME_MAP = NameMap.of(BLOCK, values()).id(v -> v.name().toLowerCase()).create();
	}
}
