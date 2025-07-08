package dev.ftb.mods.ftbquests.block.entity;

import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public abstract class BaseInputBlockEntity extends BlockEntity {
	public UUID team;
	public long task;

	public BaseInputBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
	}

	public abstract boolean canSet(Task task);

	public void selectTask(ServerPlayer entity) {
	}
}
