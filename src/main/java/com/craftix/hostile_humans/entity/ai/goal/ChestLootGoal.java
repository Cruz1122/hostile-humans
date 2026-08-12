package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ChestExtension;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

/** Minimal vanilla chest looting: search, walk, transfer useful stacks, cooldown. */
public final class ChestLootGoal extends Goal {
    private static final BlockPos UNREACHABLE = new BlockPos(0, -9999, 0);
    private static final int OPENING_TICKS = 15;
    private static final int TRANSFER_INTERVAL_TICKS = 10;
    private static final int MINIMUM_OPEN_TICKS = 45;
    private final Human human;
    private final double speedModifier;
    private BlockPos chestPos = UNREACHABLE;
    private boolean looted;
    private boolean chestOpen;
    private boolean inventoryChanged;
    private int lootingTicks;
    private int nextSlot;
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
        chestOpen = false;
        inventoryChanged = false;
        lootingTicks = 0;
        nextSlot = 0;
        travelTicks = 0;
        if (!isInInteractionRange()
                && !human.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D, speedModifier)) {
            chestPos = UNREACHABLE;
        }
    }

    @Override
    public void stop() {
        closeChest();
        human.getNavigation().stop();
        chestPos = UNREACHABLE;
    }

    @Override
    public void tick() {
        if (!eligible() || !chestStillValid()) {
            closeChest();
            chestPos = UNREACHABLE;
            return;
        }
        if (!isInInteractionRange()) {
            if (!human.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D, speedModifier)) chestPos = UNREACHABLE;
            return;
        }
        human.getNavigation().stop();
        if (!hasLineOfSight(chestPos)) {
            closeChest();
            chestPos = UNREACHABLE;
            return;
        }
        if (human.level().getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            human.getLookControl().setLookAt(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D,
                    chestPos.getZ() + 0.5D);
            if (!chestOpen) openChest(chest);
            lootingTicks++;
            if (lootingTicks >= OPENING_TICKS
                    && (lootingTicks - OPENING_TICKS) % TRANSFER_INTERVAL_TICKS == 0) {
                lootNextStack(chest);
            }
            if (looted && lootingTicks >= MINIMUM_OPEN_TICKS) finishLooting(chest);
            return;
        }
        closeChest();
        chestPos = UNREACHABLE;
    }

    private void lootNextStack(ChestBlockEntity chest) {
        HumanData data = human.getData();
        if (data == null) {
            looted = true;
            return;
        }
        while (nextSlot < chest.getContainerSize()) {
            int slot = nextSlot++;
            var source = chest.getItem(slot);
            if (source.isEmpty() || !HumanLootPolicy.isUseful(human, source)) continue;
            var transfer = source.copy();
            if (!data.storeInventoryItem(transfer)) continue;
            chest.setItem(slot, transfer);
            chest.setChanged();
            inventoryChanged = true;
            return;
        }
        looted = true;
    }

    private void finishLooting(ChestBlockEntity chest) {
        if (inventoryChanged) {
            human.queueUsefulInventoryEquipment();
            human.markEquipmentDirty();
            human.queueEquipmentReevaluation();
        }
        human.lastLootedChestPos = chest.getBlockPos();
        human.lastLootedChestTick = human.level().getGameTime();
        closeChest();
        chestPos = UNREACHABLE;
    }

    private void openChest(ChestBlockEntity chest) {
        chestOpen = true;
        ((ChestExtension) chest).hostileHumans$setForcedOpen(true);
        human.level().blockEvent(chestPos, chest.getBlockState().getBlock(), 1, 1);
    }

    private void closeChest() {
        if (!chestOpen || chestPos == UNREACHABLE) return;
        if (human.level().getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            ((ChestExtension) chest).hostileHumans$setForcedOpen(false);
            human.level().blockEvent(chestPos, chest.getBlockState().getBlock(), 1, 0);
        }
        chestOpen = false;
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
        if (human.blockPosition().distManhattan(pos) <= 2) {
            int steps = Math.max(Math.abs(pos.getX() - human.blockPosition().getX()),
                    Math.max(Math.abs(pos.getY() - human.blockPosition().getY()),
                            Math.abs(pos.getZ() - human.blockPosition().getZ())));
            for (int step = 1; step < steps; step++) {
                double progress = (double) step / steps;
                BlockPos between = new BlockPos(
                        Mth.floor(Mth.lerp(progress, human.getX(), pos.getX() + 0.5D)),
                        Mth.floor(Mth.lerp(progress, human.getY() + 1.0D, pos.getY() + 0.5D)),
                        Mth.floor(Mth.lerp(progress, human.getZ(), pos.getZ() + 0.5D)));
                if (!human.level().getBlockState(between).getCollisionShape(human.level(), between).isEmpty()) return false;
            }
            return true;
        }
        Vec3 start = human.getEyePosition();
        return clearLine(start, Vec3.atCenterOf(pos), pos)
                || clearLine(start, new Vec3(pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D), pos);
    }

    private boolean clearLine(Vec3 start, Vec3 end, BlockPos target) {
        Vec3 delta = end.subtract(start);
        int samples = Math.max(2, (int) Math.ceil(delta.length() * 8.0D));
        for (int index = 1; index < samples; index++) {
            Vec3 sample = start.add(delta.scale((double) index / samples));
            BlockPos samplePos = BlockPos.containing(sample);
            if (samplePos.equals(target)) continue;
            if (!human.level().getBlockState(samplePos).getCollisionShape(human.level(), samplePos).isEmpty()) return false;
        }
        return true;
    }

    private boolean isRecentlyLooted(BlockPos pos) {
        return human.lastLootedChestPos != null && human.lastLootedChestTick != Long.MIN_VALUE
                && human.lastLootedChestPos.equals(pos)
                && human.level().getGameTime() - human.lastLootedChestTick < Config.chestRevisitCooldownTicks.get();
    }
}
