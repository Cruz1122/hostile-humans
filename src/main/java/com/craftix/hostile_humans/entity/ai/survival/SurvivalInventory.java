package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.List;

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

    public static boolean canStore(Human human, ItemStack offered) {
        if (human.getData() == null || offered.isEmpty()) return false;
        return canStore(human, List.of(offered));
    }

    /** Simulates all inserts against one snapshot, including stack capacity. */
    public static boolean canStore(Human human, List<ItemStack> offered) {
        if (human.getData() == null) return false;
        List<ItemStack> simulated = new ArrayList<>();
        for (ItemStack stack : human.getData().getInventoryItems()) simulated.add(stack.copy());
        for (ItemStack source : offered) {
            if (source == null || source.isEmpty()) continue;
            ItemStack remaining = source.copy();
            for (int slot = 0; slot < simulated.size(); slot++) {
                if (remaining.isEmpty()) break;
                ItemStack existing = simulated.get(slot);
                if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining)) {
                    int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                    if (moved > 0) {
                        existing.grow(moved);
                        remaining.shrink(moved);
                    }
                }
            }
            for (int slot = 0; slot < simulated.size(); slot++) {
                if (remaining.isEmpty()) break;
                ItemStack existing = simulated.get(slot);
                if (existing.isEmpty()) {
                    int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    simulated.set(slot, remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    public static int insert(Human human, ItemStack offered) {
        if (human.getData() == null || offered.isEmpty()) return 0;
        int before = offered.getCount();
        human.getData().storeInventoryItem(offered);
        return before - offered.getCount();
    }
}
