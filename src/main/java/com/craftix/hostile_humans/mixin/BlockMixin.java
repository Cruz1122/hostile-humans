package com.craftix.hostile_humans.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.craftix.hostile_humans.entity.entities.Human;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

@Mixin(Block.class)
public abstract class BlockMixin {
	
	@Inject(method = "playerWillDestroy", at = @At(value = "TAIL"))
	public void useInject(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer, CallbackInfo ci) {
		if (!pLevel.isClientSide) {
			for (Human human : pLevel.getEntitiesOfClass(Human.class, new AABB(pPos).inflate(16.0))) {
				if (!human.isInvestigatingSound()) human.setInvestigateSound(pPos);
			}
		}
	}
	
	@Inject(method = "setPlacedBy", at = @At(value = "TAIL"))
	public void useInject(Level pLevel, BlockPos pPos, BlockState p_49849_, @Nullable LivingEntity entity, ItemStack p_49851_, CallbackInfo ci) {
		if (entity instanceof ServerPlayer pPlayer) {
			for (Human human : pLevel.getEntitiesOfClass(Human.class, new AABB(pPos).inflate(16.0))) {
				if (!human.isInvestigatingSound()) human.setInvestigateSound(pPos);
			}
		}
	}
}
