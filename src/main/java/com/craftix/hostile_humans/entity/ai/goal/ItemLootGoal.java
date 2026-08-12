package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

/** Walks toward nearby useful item drops so the normal pickup ability can collect them. */
public final class ItemLootGoal extends Goal {
    private static final double SEARCH_RADIUS = 12.0D;
    private static final double PICKUP_DISTANCE_SQUARED = 1.0D;
    private static final int MAX_TRAVEL_TICKS = 200;

    private final Human human;
    private final double speedModifier;
    private ItemEntity itemTarget;
    private int travelTicks;

    public ItemLootGoal(Human human, double speedModifier) {
        this.human = human;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!eligible()) return false;
        itemTarget = findNearestUsefulItem();
        return itemTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return eligible() && isValidTarget() && travelTicks++ < MAX_TRAVEL_TICKS;
    }

    @Override
    public void start() {
        travelTicks = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        human.getNavigation().stop();
        itemTarget = null;
    }

    @Override
    public void tick() {
        if (!isValidTarget()) return;
        human.getLookControl().setLookAt(itemTarget, 30.0F, 30.0F);
        if (human.distanceToSqr(itemTarget) <= PICKUP_DISTANCE_SQUARED) {
            human.getNavigation().stop();
        } else if (human.tickCount % 10 == 0 || human.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private boolean eligible() {
        return human.isAlive() && !human.isSleepingOrLyingDown() && !human.isOrderedToSit()
                && human.getTarget() == null && !human.isFleeing && human.healingAfterFleeTicks <= 0;
    }

    private ItemEntity findNearestUsefulItem() {
        AABB searchArea = human.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_RADIUS / 2.0D, SEARCH_RADIUS);
        return human.level().getEntitiesOfClass(ItemEntity.class, searchArea,
                        item -> item.isAlive() && HumanLootPolicy.isUseful(human, item.getItem()))
                .stream()
                .min(Comparator.comparingDouble(human::distanceToSqr))
                .orElse(null);
    }

    private boolean isValidTarget() {
        return itemTarget != null && itemTarget.isAlive()
                && HumanLootPolicy.isUseful(human, itemTarget.getItem())
                && human.distanceToSqr(itemTarget) <= SEARCH_RADIUS * SEARCH_RADIUS;
    }

    private void moveToTarget() {
        if (itemTarget != null) {
            human.getNavigation().moveTo(itemTarget, speedModifier);
        }
    }
}
