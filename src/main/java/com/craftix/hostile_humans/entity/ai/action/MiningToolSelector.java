package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.Optional;

public final class MiningToolSelector {
    private MiningToolSelector() {}

    public static Optional<ItemStack> select(Human human, BlockState state) {
        Comparator<ItemStack> order = Comparator
                .comparing((ItemStack stack) -> stack.isCorrectToolForDrops(state)).reversed()
                .thenComparing(Comparator.<ItemStack>comparingDouble(stack -> stack.getDestroySpeed(state)).reversed())
                .thenComparingInt(ItemStack::getDamageValue);
        java.util.stream.Stream<ItemStack> inventory = human.getData() == null
                ? java.util.stream.Stream.empty() : human.getData().getInventoryItems().stream();
        Optional<ItemStack> selected = java.util.stream.Stream.concat(java.util.stream.Stream.of(human.getMainHandItem()), inventory)
                .filter(stack -> !stack.isEmpty() && (stack.getMaxDamage() == 0 || stack.getDamageValue() < stack.getMaxDamage() - 1))
                .sorted(order).findFirst()
                .filter(stack -> Config.allowMiningWithoutCorrectTool.get() || stack.isCorrectToolForDrops(state)
                        || !state.requiresCorrectToolForDrops());
        if (selected.isPresent()) return selected;
        return !state.requiresCorrectToolForDrops() ? Optional.of(human.getMainHandItem()) : Optional.empty();
    }

    public static boolean equip(Human human, ItemStack selected) {
        if (selected == human.getMainHandItem()) return true;
        if (human.getData() == null) return false;
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            if (human.getData().getInventoryItem(slot) == selected) {
                ItemStack previous = human.getMainHandItem().copy();
                human.setItemSlot(EquipmentSlot.MAINHAND, selected.copy());
                human.getData().setInventoryItem(slot, previous);
                return true;
            }
        }
        return false;
    }
}
