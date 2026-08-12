package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

public final class PillarUpAction implements TacticalWorldAction {
    private int placed;
    private int cooldown;

    public static boolean tryPlace(Human human) {
        PillarUpAction action = new PillarUpAction();
        WorldActionContext context = new WorldActionContext(human);
        return action.canStart(context) && action.tick(context) == WorldActionResult.RUNNING;
    }

    @Override
    public boolean canStart(WorldActionContext context) {
        Human human = context.human();
        LivingEntity target = human.getTarget();
        if (!Config.enablePillaring.get() || WorldActionSupport.critical(human) || target == null || !target.isAlive()
                || placed == 0 && target.getY() <= human.getY() + 1.0D
                || target.getY() - human.getY() > Config.maxPillarHeight.get()
                || human.distanceToSqr(target) > 25.0D || placed >= Config.maxPillarBlocksPerPursuit.get()
                || !WorldActionSupport.permitted(human) || WorldActionSupport.constructionStack(human, false).isEmpty()) return false;
        BlockPos head = human.blockPosition().above(2);
        return human.level().hasChunkAt(head) && human.level().getBlockState(head).getCollisionShape(human.level(), head).isEmpty();
    }

    @Override
    public WorldActionResult tick(WorldActionContext context) {
        Human human = context.human();
        if (!canStart(context)) return WorldActionResult.FAILED;
        if (placed > 0 && human.onGround() && human.getTarget().getY() <= human.getY() + 1.0D) {
            return WorldActionResult.SUCCESS;
        }
        human.getNavigation().stop();
        human.setDeltaMovement(0.0D, human.getDeltaMovement().y, 0.0D);
        if (cooldown > 0) { cooldown--; return WorldActionResult.RUNNING; }
        if (human.onGround()) {
            human.getJumpControl().jump();
            human.getLookControl().setLookAt(human.getTarget(), 30.0F, 30.0F);
            return WorldActionResult.RUNNING;
        }
        BlockPos placePos = human.blockPosition().below();
        if (!human.level().getBlockState(placePos).canBeReplaced()
                || human.getBoundingBox().intersects(new AABB(placePos))) return WorldActionResult.RUNNING;
        Optional<net.minecraft.world.item.ItemStack> stack = WorldActionSupport.constructionStack(human, false);
        if (stack.isEmpty()) return WorldActionResult.FAILED;
        BlockState state = ((net.minecraft.world.item.BlockItem) stack.get().getItem()).getBlock().defaultBlockState();
        if (!WorldActionSupport.place(human, placePos, stack.get(), state, net.minecraft.core.Direction.UP)) return WorldActionResult.FAILED;
        placed++;
        cooldown = Config.pillarPlacementCooldownTicks.get();
        return placed >= Config.maxPillarBlocksPerPursuit.get() || human.getTarget().getY() <= human.getY() + 1.0D
                ? WorldActionResult.SUCCESS : WorldActionResult.RUNNING;
    }

    @Override public void stop(WorldActionContext context) { }
    @Override public WorldActionType type() { return WorldActionType.PILLAR_UP; }
    @Override public int placedBlockCount() { return placed; }
}
