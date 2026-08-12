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

public final class BreakObstacleAction implements TacticalWorldAction {
    private BlockPos targetBlockPos;
    private BlockState targetState;
    private float progress;
    private int lastCrackStage = -1;
    private int broken;

    public static boolean tryBreak(Human human) {
        BreakObstacleAction action = new BreakObstacleAction();
        WorldActionContext context = new WorldActionContext(human);
        if (!action.canStart(context)) return false;
        return action.tick(context) == WorldActionResult.RUNNING;
    }

    private Direction direction(Human human) {
        LivingEntity target = human.getTarget();
        if (target == null) return Direction.NORTH;
        return Math.abs(target.getX() - human.getX()) >= Math.abs(target.getZ() - human.getZ())
                ? (target.getX() >= human.getX() ? Direction.EAST : Direction.WEST)
                : (target.getZ() >= human.getZ() ? Direction.SOUTH : Direction.NORTH);
    }

    private BlockPos findObstacle(Human human) {
        Direction direction = direction(human);
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
        if (targetBlockPos == null) targetBlockPos = findObstacle(human);
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
        Optional<ItemStack> selected = MiningToolSelector.select(human, targetState);
        if (selected.isEmpty() || !MiningToolSelector.equip(human, selected.get())) return abort(human);
        ItemStack tool = human.getMainHandItem();
        boolean correct = tool.isCorrectToolForDrops(targetState);
        progress += MiningSpeedCalculator.progressPerTick(human, targetState, targetBlockPos, tool, correct);
        int stage = Math.min(9, (int) (progress * 10.0F));
        if (stage != lastCrackStage) {
            human.level().destroyBlockProgress(human.getId(), targetBlockPos, stage);
            lastCrackStage = stage;
        }
        if (progress < 1.0F) return WorldActionResult.RUNNING;
        human.level().destroyBlockProgress(human.getId(), targetBlockPos, -1);
        BlockEntity blockEntity = human.level().getBlockEntity(targetBlockPos);
        ItemStack lootTool = tool.copy();
        tool.getItem().mineBlock(tool, human.level(), targetState, targetBlockPos, human);
        if (!human.level().destroyBlock(targetBlockPos, false, human, Block.UPDATE_LIMIT)) return abort(human);
        if (human.level() instanceof ServerLevel serverLevel && correct) {
            Block.dropResources(targetState, serverLevel, targetBlockPos, blockEntity, human, lootTool);
        }
        broken++;
        targetBlockPos = null;
        targetState = null;
        progress = 0.0F;
        lastCrackStage = -1;
        human.getNavigation().recomputePath();
        return WorldActionResult.SUCCESS;
    }

    private WorldActionResult abort(Human human) {
        if (targetBlockPos != null) human.level().destroyBlockProgress(human.getId(), targetBlockPos, -1);
        targetBlockPos = null;
        targetState = null;
        progress = 0.0F;
        lastCrackStage = -1;
        return WorldActionResult.ABORTED;
    }

    @Override public void stop(WorldActionContext context) { abort(context.human()); }
    @Override public WorldActionType type() { return WorldActionType.BREAK_OBSTACLE; }
    @Override public int brokenBlockCount() { return broken; }

    private static final class Vec3Helper {
        private static net.minecraft.world.phys.Vec3 center(BlockPos pos) { return net.minecraft.world.phys.Vec3.atCenterOf(pos); }
    }
}
