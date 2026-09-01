package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalInventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Atomic access to utility items owned by a Human's persistent inventory. */
final class UtilityItemSupport {
    private UtilityItemSupport() {}

    static ItemStack find(Human human, Item item) {
        if (human.getData() != null) {
            for (ItemStack stack : human.getData().getInventoryItems()) {
                if (stack.is(item)) return stack;
            }
        }
        if (human.getMainHandItem().is(item)) return human.getMainHandItem();
        if (human.getOffhandItem().is(item)) return human.getOffhandItem();
        return ItemStack.EMPTY;
    }

    static boolean replaceOne(Human human, ItemStack source, Item replacement) {
        if (source.isEmpty()) return false;

        if (human.getData() != null) {
            for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
                if (human.getData().getInventoryItem(slot) != source) continue;
                return replaceInventoryStack(human, slot, source, replacement);
            }
        }
        if (human.getMainHandItem() == source) {
            return replaceHandStack(human, EquipmentSlot.MAINHAND, source, replacement);
        }
        if (human.getOffhandItem() == source) {
            return replaceHandStack(human, EquipmentSlot.OFFHAND, source, replacement);
        }
        return false;
    }

    static boolean consumeOne(Human human, ItemStack source) {
        if (source.isEmpty()) return false;
        if (human.getData() != null) {
            for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
                if (human.getData().getInventoryItem(slot) != source) continue;
                source.shrink(1);
                human.getData().setInventoryItem(slot, source);
                return true;
            }
        }
        if (human.getMainHandItem() == source) return consumeHandStack(human, EquipmentSlot.MAINHAND, source);
        if (human.getOffhandItem() == source) return consumeHandStack(human, EquipmentSlot.OFFHAND, source);
        return false;
    }

    private static boolean replaceInventoryStack(Human human, int slot, ItemStack source, Item replacement) {
        if (source.getCount() > 1) {
            if (!SurvivalInventory.canStore(human, new ItemStack(replacement))) return false;
            source.shrink(1);
            human.getData().setInventoryItem(slot, source);
            ItemStack remainder = new ItemStack(replacement);
            human.getData().storeInventoryItem(remainder);
        } else {
            human.getData().setInventoryItem(slot, new ItemStack(replacement));
        }
        return true;
    }

    private static boolean replaceHandStack(Human human, EquipmentSlot slot, ItemStack source, Item replacement) {
        if (source.getCount() > 1) {
            if (human.getData() == null || !SurvivalInventory.canStore(human, new ItemStack(replacement))) return false;
            source.shrink(1);
            human.setItemSlot(slot, source);
            syncHand(human, slot, source);
            ItemStack remainder = new ItemStack(replacement);
            human.getData().storeInventoryItem(remainder);
        } else {
            ItemStack replacementStack = new ItemStack(replacement);
            human.setItemSlot(slot, replacementStack);
            syncHand(human, slot, replacementStack);
        }
        return true;
    }

    private static boolean consumeHandStack(Human human, EquipmentSlot slot, ItemStack source) {
        source.shrink(1);
        human.setItemSlot(slot, source.isEmpty() ? ItemStack.EMPTY : source);
        syncHand(human, slot, source);
        return true;
    }

    private static void syncHand(Human human, EquipmentSlot slot, ItemStack stack) {
        if (human.getData() != null) {
            human.getData().setHandItem(slot == EquipmentSlot.MAINHAND ? 0 : 1, stack.copy());
        }
    }
}
