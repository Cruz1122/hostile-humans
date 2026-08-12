package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class BridgeGapAction implements TacticalWorldAction {
    private int placed;
    private int cooldown;
    private Direction direction;
    private BlockPos firstPlacePos;
    private int requiredBlocks;

    public static boolean tryPlace(Human human) {
        BridgeGapAction action = new BridgeGapAction();
        WorldActionContext context = new WorldActionContext(human);
        return action.canStart(context) && action.tick(context) == WorldActionResult.RUNNING;
    }

    private Direction direction(Human human) {
        LivingEntity target = human.getTarget();
        if (target == null) return Direction.NORTH;
        return Math.abs(target.getX() - human.getX()) >= Math.abs(target.getZ() - human.getZ())
                ? (target.getX() >= human.getX() ? Direction.EAST : Direction.WEST)
                : (target.getZ() >= human.getZ() ? Direction.SOUTH : Direction.NORTH);
    }

    private BlockPos next(Human human, Direction dir, int distance) {
        return human.blockPosition().relative(dir, distance).below();
    }

    @Override
    public boolean canStart(WorldActionContext context) {
        Human human = context.human();
        LivingEntity target = human.getTarget();
        direction = direction(human);
        if (!Config.enableBridging.get() || WorldActionSupport.critical(human) || target == null || !target.isAlive()
                || !WorldActionSupport.permitted(human) || WorldActionSupport.constructionStack(human, true).isEmpty()
                || !Config.allowBridgeOverLava.get() && (human.isInLava() || target.isInLava())) return false;
        BlockPos belowFeet = next(human, direction, 0);
        int firstDistance = human.level().getBlockState(belowFeet).isAir() ? 0 : 1;
        BlockPos first = next(human, direction, firstDistance);
        if (!human.level().hasChunkAt(first) || !human.level().getBlockState(first).isAir()) return false;
        for (int gapLength = 1; gapLength <= Config.maxBridgeLength.get(); gapLength++) {
            BlockPos landing = first.relative(direction, gapLength);
            if (!human.level().hasChunkAt(landing)) return false;
            BlockState below = human.level().getBlockState(landing);
            if (!below.getCollisionShape(human.level(), landing).isEmpty()
                    && human.blockPosition().distSqr(landing) <= 25.0D) {
                firstPlacePos = first;
                requiredBlocks = gapLength;
                return true;
            }
        }
        return false;
    }

    @Override
    public WorldActionResult tick(WorldActionContext context) {
        Human human = context.human();
        if (placed == 0 && !canStart(context)) return WorldActionResult.FAILED;
        if (placed > 0 && (WorldActionSupport.critical(human) || !WorldActionSupport.permitted(human)
                || human.getTarget() == null || !human.getTarget().isAlive())) return WorldActionResult.ABORTED;
        if (cooldown > 0) { cooldown--; return WorldActionResult.RUNNING; }
        BlockPos placePos = firstPlacePos.relative(direction, placed);
        Optional<net.minecraft.world.item.ItemStack> stack = WorldActionSupport.constructionStack(human, true);
        if (stack.isEmpty()) return WorldActionResult.FAILED;
        BlockState state = ((net.minecraft.world.item.BlockItem) stack.get().getItem()).getBlock().defaultBlockState();
        if (!WorldActionSupport.place(human, placePos, stack.get(), state, direction.getOpposite())) return WorldActionResult.FAILED;
        placed++;
        cooldown = Config.bridgePlacementCooldownTicks.get();
        human.getNavigation().moveTo(placePos.getX() + 0.5D, human.getY(), placePos.getZ() + 0.5D, 1.0D);
        return placed >= requiredBlocks ? WorldActionResult.SUCCESS : WorldActionResult.RUNNING;
    }

    @Override public void stop(WorldActionContext context) { }
    @Override public WorldActionType type() { return WorldActionType.BRIDGE_GAP; }
    @Override public int placedBlockCount() { return placed; }
}
