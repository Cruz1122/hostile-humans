package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Predicate;

public final class SurvivalInventory {
    private SurvivalInventory() {}

    public static int count(Human human, Predicate<ItemStack> predicate) {
        int total = 0;
        HumanData data = human.getData();
        if (data != null) {
            for (ItemStack stack : data.getInventoryItems()) if (predicate.test(stack)) total += stack.getCount();
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = human.getItemBySlot(slot);
            if (predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }

    public static int count(Human human, ItemLike item) {
        return count(human, stack -> stack.is(item.asItem()));
    }

    public static boolean contains(Human human, Predicate<ItemStack> predicate) {
        return count(human, predicate) > 0;
    }

    public static int inventoryCount(Human human, Predicate<ItemStack> predicate) {
        if (human.getData() == null) return 0;
        int total = 0;
        for (ItemStack stack : human.getData().getInventoryItems()) if (predicate.test(stack)) total += stack.getCount();
        return total;
    }

    public static int removeInventory(Human human, Predicate<ItemStack> predicate, int requested) {
        if (requested <= 0 || human.getData() == null) return 0;
        int removed = 0;
        for (int slot = 0; slot < human.getData().getInventoryItemsSize() && removed < requested; slot++) {
            ItemStack stack = human.getData().getInventoryItem(slot);
            if (!predicate.test(stack)) continue;
            int amount = Math.min(requested - removed, stack.getCount());
            stack.shrink(amount);
            if (stack.isEmpty()) human.getData().setInventoryItem(slot, ItemStack.EMPTY);
            removed += amount;
        }
        return removed;
    }

    public static boolean canStore(Human human, ItemStack offered) {
        if (human.getData() == null || offered.isEmpty()) return false;
        ItemStack probe = offered.copy();
        HumanData data = human.getData();
        int firstPickupSlot = data.getInventoryItemsSize() - 10;
        for (int slot = firstPickupSlot; slot < data.getInventoryItemsSize(); slot++) {
            ItemStack existing = data.getInventoryItem(slot);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(existing, probe) && existing.getCount() < existing.getMaxStackSize()) return true;
        }
        return false;
    }

    public static int insert(Human human, ItemStack offered) {
        if (human.getData() == null || offered.isEmpty()) return 0;
        int before = offered.getCount();
        human.getData().storeInventoryItem(offered);
        return before - offered.getCount();
    }
}
