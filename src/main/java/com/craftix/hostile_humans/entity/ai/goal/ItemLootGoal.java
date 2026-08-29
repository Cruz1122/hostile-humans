package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.ai.survival.SurvivalInventory;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeedsEvaluator;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

/** Walks toward nearby useful item drops so the normal pickup ability can collect them. */
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
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
        if (!isValidTarget()) return;
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
        human.getLookControl().setLookAt(itemTarget, 30.0F, 30.0F);
        if (distanceSqr <= PICKUP_DISTANCE_SQUARED) {
            collectTarget();
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
                        this::canPickUp)
                .stream()
                .filter(this::hasPickupReachablePath)
                .min(Comparator.comparingDouble(human::distanceToSqr))
                .orElse(null);
    }

    private boolean hasPickupReachablePath(ItemEntity item) {
        if (Math.abs(human.getY() - item.getY()) <= 1.0D && human.distanceToSqr(item) <= 2.25D) return true;
        var path = human.getNavigation().createPath(item, 0);
        if (path == null || !path.canReach() || path.getEndNode() == null) return false;
        var end = path.getEndNode();
        if (Math.abs(end.y - human.getY()) > 1.5D) return false;
        return Math.abs(item.getY() - end.y) <= 1.0D
                && item.distanceToSqr(end.x + 0.5D, end.y, end.z + 0.5D) <= 2.25D;
    }

    private boolean isValidTarget() {
        return itemTarget != null && canPickUp(itemTarget)
                && human.distanceToSqr(itemTarget) <= SEARCH_RADIUS * SEARCH_RADIUS;
    }

    private boolean canPickUp(ItemEntity item) {
        return item.isAlive() && !item.hasPickUpDelay() && !isTemporarilyIgnored(item)
                && HumanLootPolicy.isUseful(human, item.getItem())
                && SurvivalInventory.canStore(human, item.getItem());
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
        if (itemTarget.hasPickUpDelay()) return;
        ItemStack stack = itemTarget.getItem();
        int inserted = SurvivalInventory.insert(human, stack);
        if (inserted <= 0) {
            ignoreCurrentTarget();
            itemTarget = null;
            return;
        }
        if (stack.isEmpty()) itemTarget.discard();
        else itemTarget.setItem(stack);
        human.markEquipmentDirty();
        human.queueUsefulInventoryEquipment();
        human.queueEquipmentReevaluation();
        SquadNeedsEvaluator.invalidate(human);
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
