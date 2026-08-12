package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

/** Minimal vanilla chest looting: search, walk, transfer useful stacks, cooldown. */
public final class ChestLootGoal extends Goal {
    private static final BlockPos UNREACHABLE = new BlockPos(0, -9999, 0);
    private final Human human;
    private final double speedModifier;
    private BlockPos chestPos = UNREACHABLE;
    private boolean looted;
    private int travelTicks;

    public ChestLootGoal(Human human, double speedModifier) {
        this.human = human;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!eligible() || human.lookForChestCooldown > 0) return false;
        human.lookForChestCooldown = Config.chestSearchIntervalTicks.get();
        chestPos = findVisibleChest();
        return chestPos != UNREACHABLE;
    }

    @Override
    public boolean canContinueToUse() {
        if (!eligible() || chestPos == UNREACHABLE || !chestStillValid()) return false;
        if (isInInteractionRange()) return true;
        return travelTicks++ < 200;
    }

    @Override
    public void start() {
        looted = false;
        travelTicks = 0;
        if (!isInInteractionRange()
                && !human.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D, speedModifier)) {
            chestPos = UNREACHABLE;
        }
    }

    @Override
    public void stop() {
        human.getNavigation().stop();
        chestPos = UNREACHABLE;
    }

    @Override
    public void tick() {
        if (!eligible() || !chestStillValid()) {
            chestPos = UNREACHABLE;
            return;
        }
        if (!isInInteractionRange()) {
            if (!human.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D, speedModifier)) chestPos = UNREACHABLE;
            return;
        }
        human.getNavigation().stop();
        if (!hasLineOfSight(chestPos)) {
            chestPos = UNREACHABLE;
            return;
        }
        if (human.level().getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            loot(chest);
            looted = true;
        }
        chestPos = UNREACHABLE;
    }

    private void loot(ChestBlockEntity chest) {
        HumanData data = human.getData();
        if (data == null) return;
        boolean changed = false;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            var source = chest.getItem(slot);
            if (source.isEmpty() || !HumanLootPolicy.isUseful(human, source)) continue;
            var transfer = source.copy();
            if (!data.storeInventoryItem(transfer)) continue;
            chest.setItem(slot, transfer);
            changed = true;
        }
        if (changed) {
            chest.setChanged();
            human.markEquipmentDirty();
            human.queueEquipmentReevaluation();
        }
        human.lastLootedChestPos = chest.getBlockPos();
        human.lastLootedChestTick = human.level().getGameTime();
    }

    private boolean eligible() {
        return Config.enableChestLooting.get() && human.isAlive() && human.getTarget() == null
                && !human.isFleeing && human.healingAfterFleeTicks <= 0
                && human.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private BlockPos findVisibleChest() {
        int radius = Config.chestSearchRadius.get();
        BlockPos origin = human.blockPosition();
        BlockPos nearest = UNREACHABLE;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (!human.level().hasChunkAt(pos) || !human.level().getBlockState(pos).is(Blocks.CHEST)) continue;
            double distance = human.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance > radius * radius || distance >= nearestDistance || isRecentlyLooted(pos)) continue;
            if (hasLineOfSight(pos)) {
                nearest = pos.immutable();
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean chestStillValid() {
        return human.level().hasChunkAt(chestPos) && human.level().getBlockState(chestPos).is(Blocks.CHEST)
                && human.level().getBlockEntity(chestPos) instanceof ChestBlockEntity;
    }

    private boolean isInInteractionRange() { return human.distanceToSqr(Vec3.atCenterOf(chestPos)) <= 9.0D; }

    private boolean hasLineOfSight(BlockPos pos) {
        Vec3 start = human.getEyePosition();
        return seesChest(start, Vec3.atCenterOf(pos), pos)
                || seesChest(start, new Vec3(pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D), pos);
    }

    private boolean seesChest(Vec3 start, Vec3 end, BlockPos pos) {
        var hit = human.level().clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, human));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                || (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos));
    }

    private boolean isRecentlyLooted(BlockPos pos) {
        return human.lastLootedChestPos != null && human.lastLootedChestTick != Long.MIN_VALUE
                && human.lastLootedChestPos.equals(pos)
                && human.level().getGameTime() - human.lastLootedChestTick < Config.chestRevisitCooldownTicks.get();
    }
}
