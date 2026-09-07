package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.ai.survival.LootCollector;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalQueryBudget;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeed;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeedsEvaluator;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

/** Walks toward nearby useful item drops and transfers them into survival inventory at pickup range. */
public final class ItemLootGoal extends Goal {
    private static final double SEARCH_RADIUS = 12.0D;
    private static final double PICKUP_DISTANCE_SQUARED = 2.25D;
    private static final int MAX_TRAVEL_TICKS = 200;
    private static final int MAX_STALLED_TICKS = 40;

    private final Human human;
    private final double speedModifier;
    private ItemEntity itemTarget;
    private int travelTicks;
    private int stalledTicks;
    private double closestDistanceSqr;
    private int ignoredEntityId = -1;
    private int ignoredUntilTick;
    private int nextSearchTick;

    public ItemLootGoal(Human human, double speedModifier) {
        this.human = human;
        this.speedModifier = speedModifier;
        // Navigation does not require forcing the head toward the item. Keeping
        // LOOK free avoids the visibly broken "stare at loot" behavior.
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!eligible() || human.tickCount < nextSearchTick) return false;
        nextSearchTick = human.tickCount + 10 + Math.floorMod(human.getUUID().hashCode(), 5);
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
        stalledTicks = 0;
        closestDistanceSqr = human.distanceToSqr(itemTarget);
        moveToTarget();
    }

    @Override
    public void stop() {
        human.getNavigation().stop();
        itemTarget = null;
    }

    @Override
    public void tick() {
        if (!isValidTarget()) {
            // A hunt drop can exist before its vanilla pickup delay expires, or
            // become uncollectable after another inventory change. Do not keep
            // looking at or navigating toward a target that cannot be picked up.
            itemTarget = null;
            human.getNavigation().stop();
            return;
        }
        double distanceSqr = human.distanceToSqr(itemTarget);
        if (distanceSqr + 0.25D < closestDistanceSqr) {
            closestDistanceSqr = distanceSqr;
            stalledTicks = 0;
        } else if (++stalledTicks >= MAX_STALLED_TICKS) {
            ignoreCurrentTarget();
            itemTarget = null;
            human.getNavigation().stop();
            return;
        }
        if (itemTarget.hasPickUpDelay()) {
            if (distanceSqr <= PICKUP_DISTANCE_SQUARED) {
                human.getNavigation().stop();
                stalledTicks = 0;
            } else if (human.tickCount % 10 == 0 || human.getNavigation().isDone()) {
                moveToTarget();
            }
            return;
        }
        if (distanceSqr <= PICKUP_DISTANCE_SQUARED) {
            collectTarget();
        } else if (human.tickCount % 10 == 0 || human.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private boolean eligible() {
        return human.isAlive() && !human.isSleepingOrLyingDown() && !human.isOrderedToSit()
                && !human.isFleeing && human.healingAfterFleeTicks <= 0;
    }

    private boolean inCombat() {
        return human.getTarget() != null;
    }

    private ItemEntity findNearestUsefulItem() {
        AABB searchArea = human.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_RADIUS / 2.0D, SEARCH_RADIUS);
        boolean needsFood = SquadNeedsEvaluator.evaluate(human).needs(SquadNeed.FOOD);
        // Keep survival/crafting collection from being pre-empted by every
        // tool drop. A ranged or empty hand still makes a melee candidate the
        // highest-priority pickup.
        boolean needsMelee = inCombat();
        return human.level().getEntitiesOfClass(ItemEntity.class, searchArea,
                        this::canPickUp)
                .stream()
                .filter(this::hasPickupReachablePath)
                .min(Comparator.comparingInt((ItemEntity item) -> {
                            if (needsMelee && MeleeWeaponSelector.isMeleeCandidate(item.getItem())) return 0;
                            if (!needsMelee && needsFood && item.getItem().getFoodProperties(null) != null) return 1;
                            return 2;
                        })
                        .thenComparingDouble(human::distanceToSqr))
                .orElse(null);
    }

    private boolean hasPickupReachablePath(ItemEntity item) {
        if (Math.abs(human.getY() - item.getY()) <= 1.0D && human.distanceToSqr(item) <= 2.25D) return true;
        if (!SurvivalQueryBudget.tryPath(human)) return false;
        var path = human.getNavigation().createPath(item, 0);
        if (path == null || !path.canReach() || path.getEndNode() == null) return false;
        var end = path.getEndNode();
        return Math.abs(item.getY() - end.y) <= 1.0D
                && item.distanceToSqr(end.x + 0.5D, end.y, end.z + 0.5D) <= 2.25D;
    }

    private boolean isValidTarget() {
        return itemTarget != null && canPickUp(itemTarget)
                && human.distanceToSqr(itemTarget) <= SEARCH_RADIUS * SEARCH_RADIUS;
    }

    private boolean canPickUp(ItemEntity item) {
        return item.isAlive() && !isTemporarilyIgnored(item)
                && LootCollector.canCollect(human, item.getItem());
    }

    private boolean isTemporarilyIgnored(ItemEntity item) {
        return item.getId() == ignoredEntityId && human.tickCount < ignoredUntilTick;
    }

    private void ignoreCurrentTarget() {
        if (itemTarget == null) return;
        ignoredEntityId = itemTarget.getId();
        ignoredUntilTick = human.tickCount + 200;
    }

    private void collectTarget() {
        if (itemTarget == null) return;
        int inserted = LootCollector.collect(human, itemTarget);
        if (inserted <= 0) {
            ignoreCurrentTarget();
            itemTarget = null;
            return;
        }
        itemTarget = null;
        nextSearchTick = human.tickCount;
        human.getNavigation().stop();
    }

    private void moveToTarget() {
        if (itemTarget != null) {
            human.getNavigation().moveTo(itemTarget, speedModifier);
        }
    }
}
