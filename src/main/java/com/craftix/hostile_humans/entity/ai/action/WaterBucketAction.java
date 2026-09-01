package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

/** Places one source block to rescue a Human, then recovers only that source. */
public final class WaterBucketAction {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private WaterBucketAction() {}

    public static boolean tryExtinguish(Human human) {
        if (human.level().isClientSide || human.level().dimensionType().ultraWarm()
                || !Config.enableWaterBucketTactics.get()
                || human.getEffect(MobEffects.FIRE_RESISTANCE) != null
                || !human.isOnFire() && findNearbyFire(human) == null
                || !WorldActionSupport.permitted(human)) return false;

        ItemStack bucket = UtilityItemSupport.find(human, Items.WATER_BUCKET);
        if (bucket.isEmpty()) return false;

        BlockPos fire = findNearbyFire(human);
        if (fire != null && place(human, fire, bucket)) {
            human.clearFire();
            return true;
        }

        for (Direction direction : HORIZONTAL) {
            BlockPos candidate = human.blockPosition().relative(direction);
            if (place(human, candidate, bucket)) {
                human.clearFire();
                return true;
            }
        }
        return false;
    }

    public static boolean tryRecover(Human human) {
        if (human.level().isClientSide || human.level().dimensionType().ultraWarm()
                || !Config.enableWaterBucketTactics.get()
                || human.getWaterSourcePos() == null || human.waterRecoveryCooldown > 0
                || !WorldActionSupport.permitted(human)) return false;

        BlockPos sourcePos = human.getWaterSourcePos();
        if (human.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(sourcePos)) > 36.0D) return false;
        if (!human.level().hasChunkAt(sourcePos)
                || !human.level().getFluidState(sourcePos).is(net.minecraft.tags.FluidTags.WATER)
                || !human.level().getFluidState(sourcePos).isSource()) {
            human.clearWaterSourcePos();
            return false;
        }

        ItemStack emptyBucket = UtilityItemSupport.find(human, Items.BUCKET);
        if (emptyBucket.isEmpty()) return false;
        BlockState previous = human.level().getBlockState(sourcePos);
        if (!human.level().setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return false;

        if (!UtilityItemSupport.replaceOne(human, emptyBucket, Items.WATER_BUCKET)) {
            human.level().setBlock(sourcePos, previous, Block.UPDATE_ALL);
            return false;
        }
        human.level().gameEvent(GameEvent.BLOCK_DESTROY, sourcePos, GameEvent.Context.of(human, previous));
        human.clearWaterSourcePos();
        return true;
    }

    private static BlockPos findNearbyFire(Human human) {
        BlockPos center = human.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            BlockState state = human.level().getBlockState(pos);
            if ((state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE))
                    && !human.getBoundingBox().intersects(new AABB(pos))) return pos.immutable();
        }
        return null;
    }

    private static boolean place(Human human, BlockPos pos, ItemStack bucket) {
        if (!human.level().hasChunkAt(pos) || human.getBoundingBox().intersects(new AABB(pos))) return false;
        BlockState existing = human.level().getBlockState(pos);
        if (!existing.canBeReplaced() && !existing.is(Blocks.FIRE) && !existing.is(Blocks.SOUL_FIRE)) return false;
        if (human.level().getBlockEntity(pos) != null) return false;
        if (!human.level().getEntities(human, new AABB(pos)).isEmpty()) return false;

        BlockState water = Blocks.WATER.defaultBlockState();
        BlockSnapshot snapshot = BlockSnapshot.create(human.level().dimension(), human.level(), pos, Block.UPDATE_ALL);
        if (ForgeEventFactory.onBlockPlace(human, snapshot, Direction.UP)
                || !human.level().setBlock(pos, water, Block.UPDATE_ALL)) return false;
        if (!UtilityItemSupport.replaceOne(human, bucket, Items.BUCKET)) {
            human.level().setBlock(pos, existing, Block.UPDATE_ALL);
            return false;
        }
        human.level().gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(human, water));
        human.level().playSound(null, pos, water.getSoundType(human.level(), pos, human).getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
        human.setWaterSourcePos(pos);
        human.waterRecoveryCooldown = Config.waterRecoveryDelayTicks.get();
        return true;
    }
}
