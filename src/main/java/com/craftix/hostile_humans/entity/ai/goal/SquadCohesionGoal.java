package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/** Low-priority rejoin behavior; deliberately has no leader or formation model. */
public final class SquadCohesionGoal extends Goal {
    private static final double REJOIN_DISTANCE_SQUARED = 20.0D * 20.0D;
    private static final double IDEAL_DISTANCE_SQUARED = 10.0D * 10.0D;
    private final Human human;
    private final double speedModifier;
    private Vec3 center;

    public SquadCohesionGoal(Human human, double speedModifier) {
        this.human = human;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!idle() || Math.floorMod(human.tickCount + human.getId(), 30) != 0) return false;
        List<Human> members = SquadManager.nearbyMembers(human);
        if (members.isEmpty()) return false;
        center = centerOf(members);
        return human.distanceToSqr(center) > REJOIN_DISTANCE_SQUARED;
    }

    @Override
    public boolean canContinueToUse() {
        return idle() && center != null && human.distanceToSqr(center) > IDEAL_DISTANCE_SQUARED
                && !human.getNavigation().isDone();
    }

    @Override
    public void start() {
        human.getNavigation().moveTo(center.x, center.y, center.z, speedModifier);
    }

    @Override
    public void stop() {
        human.getNavigation().stop();
        center = null;
    }

    private boolean idle() {
        return human.getTarget() == null && !human.isFleeing && human.healingAfterFleeTicks <= 0
                && !human.isUsingItem() && !human.isSleepingOrLyingDown() && !human.isInvestigatingSound()
                && !human.hasFreshSquadThreatMemory();
    }

    private Vec3 centerOf(List<Human> members) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Human member : members) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
        }
        double count = members.size();
        return new Vec3(x / count, y / count, z / count);
    }
}
