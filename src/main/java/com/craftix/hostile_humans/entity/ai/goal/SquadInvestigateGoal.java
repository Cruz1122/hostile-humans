package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Moves to fresh squad intelligence, then lets normal perception take over. */
public final class SquadInvestigateGoal extends Goal {
    private final Human human;
    private final double speedModifier;
    private BlockPos destination;

    public SquadInvestigateGoal(Human human, double speedModifier) {
        this.human = human;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!idleEnough() || !human.hasFreshSquadThreatMemory()) return false;
        destination = human.getLastKnownSquadTargetPos();
        return destination != null && human.blockPosition().distSqr(destination) > 9.0D
                && human.blockPosition().distSqr(destination) <= SquadManager.COOPERATION_RADIUS * SquadManager.COOPERATION_RADIUS;
    }

    @Override
    public boolean canContinueToUse() {
        return idleEnough() && human.hasFreshSquadThreatMemory() && destination != null
                && human.blockPosition().distSqr(destination) > 9.0D && !human.getNavigation().isDone();
    }

    @Override
    public void start() {
        moveToDestination();
    }

    @Override
    public void tick() {
        BlockPos latest = human.getLastKnownSquadTargetPos();
        if (latest != null && !latest.equals(destination)) destination = latest;
        if (human.tickCount % 10 == 0 || human.getNavigation().isDone()) moveToDestination();
    }

    @Override
    public void stop() {
        human.getNavigation().stop();
        destination = null;
    }

    private boolean idleEnough() {
        return human.getTarget() == null && !human.isFleeing && human.healingAfterFleeTicks <= 0
                && !human.isUsingItem() && !human.isSleepingOrLyingDown();
    }

    private void moveToDestination() {
        if (destination != null) {
            human.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
                    speedModifier);
        }
    }
}
