package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

/** Small, server-side retreat action; deliberately not a general block builder. */
public final class PlaceCobwebAction {
    private PlaceCobwebAction() {
    }

    public static boolean tryPlace(Human human) {
        if (human.level().isClientSide || !Config.enableCobwebPlacement.get()
                || !human.isFleeing || human.getTarget() != null
                || human.toAvoid == null || !human.toAvoid.isAlive()
                || human.cobwebCooldown > 0
                || human.cobwebsPlacedThisCombat >= Config.maxCobwebsPerCombat.get()) {
            return false;
        }
        if (!ForgeEventFactory.getMobGriefingEvent(human.level(), human)) {
            return false;
        }
        LivingEntity threat = human.toAvoid;
        if (human.distanceToSqr(threat) > 15D * 15D) {
            return false;
        }

        ItemStack cobwebs = findCobweb(human);
        if (cobwebs.isEmpty()) {
            return false;
        }

        BlockPos candidate = candidateFor(human);
        if (candidate == null) {
            return false;
        }
        return tryPlaceAt(human, threat, cobwebs, candidate);
    }

    public static BlockPos candidateFor(Human human) {
        if (human.toAvoid == null) {
            return null;
        }
        Vec3 away = human.position().subtract(human.toAvoid.position());
        if (away.lengthSqr() < 0.01D) {
            return null;
        }
        away = away.normalize();
        return BlockPos.containing(
                human.getX() + away.x * 1.5D, human.getY(), human.getZ() + away.z * 1.5D);
    }

    private static ItemStack findCobweb(Human human) {
        if (human.getMainHandItem().is(Items.COBWEB)) {
            return human.getMainHandItem();
        }
        if (human.getOffhandItem().is(Items.COBWEB)) {
            return human.getOffhandItem();
        }
        if (human.getData() != null) {
            for (ItemStack stack : human.getData().getInventoryItems()) {
                if (stack.is(Items.COBWEB)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryPlaceAt(Human human, LivingEntity threat, ItemStack cobwebs, BlockPos pos) {
        Level level = human.level();
        if (!level.hasChunkAt(pos) || human.distanceToSqr(Vec3.atCenterOf(pos)) > Config.cobwebPlacementReach.get() * Config.cobwebPlacementReach.get()) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        BlockState placed = Blocks.COBWEB.defaultBlockState();
        if (!existing.isAir() || !placed.canSurvive(level, pos)
                || level.getBlockEntity(pos) != null
                || human.getBoundingBox().intersects(new AABB(pos))
                || threat.getBoundingBox().intersects(new AABB(pos))) {
            return false;
        }
        if (!level.getEntities(human, new AABB(pos)).isEmpty()) {
            return false;
        }
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(nearby).is(Blocks.COBWEB)) {
                return false;
            }
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos, Block.UPDATE_ALL);
        if (ForgeEventFactory.onBlockPlace(human, snapshot, Direction.UP)
                || !level.setBlock(pos, placed, Block.UPDATE_ALL)) {
            return false;
        }
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(human, placed));
        level.playSound(null, pos, placed.getSoundType(level, pos, human).getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
        cobwebs.shrink(1);
        human.cobwebCooldown = Config.cobwebCooldownTicks.get();
        human.cobwebsPlacedThisCombat++;
        return true;
    }
}
