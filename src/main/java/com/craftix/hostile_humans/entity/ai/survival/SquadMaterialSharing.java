package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public final class SquadMaterialSharing {
    private static final double SHARE_DISTANCE_SQUARED = 6.0D * 6.0D;

    private SquadMaterialSharing() {}

    public static int transfer(Human donor, Human receiver, Predicate<ItemStack> material, int requested, int reserve) {
        if (requested <= 0 || donor.getData() == null || receiver.getData() == null
                || !SquadManager.canShareSquad(donor, receiver)
                || donor.distanceToSqr(receiver) > SHARE_DISTANCE_SQUARED) return 0;
        int available = Math.max(0, SurvivalInventory.inventoryCount(donor, material) - reserve);
        int remaining = Math.min(requested, available);
        int transferred = 0;
        for (int slot = 0; slot < donor.getData().getInventoryItemsSize() && remaining > 0; slot++) {
            ItemStack source = donor.getData().getInventoryItem(slot);
            if (!material.test(source)) continue;
            int offeredCount = Math.min(source.getCount(), remaining);
            ItemStack offered = source.copyWithCount(offeredCount);
            int inserted = SurvivalInventory.insert(receiver, offered);
            if (inserted <= 0) continue;
            source.shrink(inserted);
            if (source.isEmpty()) donor.getData().setInventoryItem(slot, ItemStack.EMPTY);
            remaining -= inserted;
            transferred += inserted;
        }
        if (transferred > 0) {
            SquadNeedsEvaluator.invalidate(donor);
            receiver.markEquipmentDirty();
            receiver.queueUsefulInventoryEquipment();
            receiver.queueEquipmentReevaluation();
        }
        return transferred;
    }
}
