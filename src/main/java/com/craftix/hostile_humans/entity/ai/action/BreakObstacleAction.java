package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import com.craftix.hostile_humans.entity.ai.survival.ProgressiveBlockBreaker;

public final class BreakObstacleAction implements TacticalWorldAction {
    private BlockPos targetBlockPos;
    private BlockState targetState;
    private ProgressiveBlockBreaker breaker;
    private int broken;

    public static boolean tryBreak(Human human) {
        BreakObstacleAction action = new BreakObstacleAction();
        WorldActionContext context = new WorldActionContext(human);
        if (!action.canStart(context)) return false;
        return action.tick(context) == WorldActionResult.RUNNING;
    }

    private Direction direction(WorldActionContext context) {
        Human human = context.human();
        if (context.objective() != null) {
            BlockPos target = context.objective();
            return Math.abs(target.getX() - human.getBlockX()) >= Math.abs(target.getZ() - human.getBlockZ())
                    ? (target.getX() >= human.getBlockX() ? Direction.EAST : Direction.WEST)
                    : (target.getZ() >= human.getBlockZ() ? Direction.SOUTH : Direction.NORTH);
        }
        LivingEntity target = human.getTarget();
        if (target == null) return Direction.NORTH;
        return Math.abs(target.getX() - human.getX()) >= Math.abs(target.getZ() - human.getZ())
                ? (target.getX() >= human.getX() ? Direction.EAST : Direction.WEST)
                : (target.getZ() >= human.getZ() ? Direction.SOUTH : Direction.NORTH);
    }

    private BlockPos findObstacle(WorldActionContext context) {
        Human human = context.human();
        Direction direction = direction(context);
        BlockPos base = human.blockPosition().relative(direction);
        for (int y = 0; y <= 1; y++) {
            BlockPos candidate = base.above(y);
            if (!human.level().hasChunkAt(candidate)) return null;
            BlockState state = human.level().getBlockState(candidate);
            if (state.is(TacticalTags.NAVIGATION_BREAKABLE) && !state.is(TacticalTags.NEVER_BREAK)
                    && !state.hasBlockEntity() && state.getDestroySpeed(human.level(), candidate) >= 0.0F) return candidate;
        }
        return null;
    }

    @Override
    public boolean canStart(WorldActionContext context) {
        Human human = context.human();
        if (!Config.enableNavigationMining.get() || WorldActionSupport.critical(human) || human.getTarget() == null
                || !human.getTarget().isAlive() || !WorldActionSupport.permitted(human)
                || broken >= Config.maxMiningBlocksPerRecovery.get()
                || human.getData() == null || human.getData().getInventoryItems().isEmpty()) return false;
        if (targetBlockPos == null) targetBlockPos = findObstacle(context);
        if (targetBlockPos == null) return false;
        targetState = human.level().getBlockState(targetBlockPos);
        return !targetState.isAir() && !targetState.is(TacticalTags.NEVER_BREAK) && !targetState.hasBlockEntity()
                && human.distanceToSqr(Vec3Helper.center(targetBlockPos)) <= 25.0D
                && MiningToolSelector.select(human, targetState).isPresent();
    }

    @Override
    public WorldActionResult tick(WorldActionContext context) {
        Human human = context.human();
        if (!canStart(context)) return WorldActionResult.FAILED;
        if (!human.level().getBlockState(targetBlockPos).equals(targetState)) return abort(human);
        if (breaker == null) breaker = new ProgressiveBlockBreaker(human, targetBlockPos, false,
                !Config.allowMiningWithoutCorrectTool.get());
        WorldActionResult result = breaker.tick();
        if (result == WorldActionResult.RUNNING) return result;
        if (result != WorldActionResult.SUCCESS) return abort(human);
        broken++;
        targetBlockPos = null;
        targetState = null;
        breaker = null;
        human.getNavigation().recomputePath();
        return WorldActionResult.SUCCESS;
    }

    private WorldActionResult abort(Human human) {
        if (breaker != null) breaker.abort();
        targetBlockPos = null;
        targetState = null;
        breaker = null;
        return WorldActionResult.ABORTED;
    }

    @Override public void stop(WorldActionContext context) { abort(context.human()); }
    @Override public WorldActionType type() { return WorldActionType.BREAK_OBSTACLE; }
    @Override public int brokenBlockCount() { return broken; }

    private static final class Vec3Helper {
        private static net.minecraft.world.phys.Vec3 center(BlockPos pos) { return net.minecraft.world.phys.Vec3.atCenterOf(pos); }
    }
}
