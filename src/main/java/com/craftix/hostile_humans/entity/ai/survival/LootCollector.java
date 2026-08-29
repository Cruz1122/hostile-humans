package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Single authoritative path for transferring a world item into survival storage. */
public final class LootCollector {
    private LootCollector() {}

    public static int collect(Human human, ItemEntity entity) {
        if (entity == null || !entity.isAlive() || entity.hasPickUpDelay()) return 0;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || !HumanLootPolicy.isUseful(human, stack)) return 0;
        int inserted = SurvivalInventory.insert(human, stack);
        if (inserted <= 0) return 0;
        if (stack.isEmpty()) entity.discard();
        else entity.setItem(stack);
        human.markEquipmentDirty();
        human.queueUsefulInventoryEquipment();
        human.queueEquipmentReevaluation();
        SquadNeedsEvaluator.invalidate(human);
        return inserted;
    }
}
