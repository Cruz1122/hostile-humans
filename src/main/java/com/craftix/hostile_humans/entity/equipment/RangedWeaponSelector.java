package com.craftix.hostile_humans.entity.equipment;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Comparator;
import java.util.Optional;

/** Deterministic selector for usable bows and crossbows. */
public final class RangedWeaponSelector {
    private RangedWeaponSelector() {
    }

    public static Optional<Candidate> select(Human human) {
        Candidate best = null;
        ItemStack mainhand = human.getMainHandItem();
        if (isUsable(human, mainhand)) {
            best = new Candidate(-1, mainhand, score(mainhand));
        }

        if (human.getData() == null) return Optional.ofNullable(best);
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            ItemStack stack = human.getData().getInventoryItem(slot);
            if (!isUsable(human, stack)) continue;
            Candidate candidate = new Candidate(slot, stack, score(stack));
            if (best == null || Candidate.ORDER.compare(candidate, best) < 0) best = candidate;
        }
        return Optional.ofNullable(best);
    }

    public static boolean equipBest(Human human) {
        Optional<Candidate> selected = select(human);
        if (selected.isEmpty() || selected.get().inventorySlot() < 0) return false;

        Candidate candidate = selected.get();
        ItemStack previous = human.getMainHandItem().copy();
        human.setItemSlot(EquipmentSlot.MAINHAND, candidate.stack().copy());
        human.getData().setInventoryItem(candidate.inventorySlot(), previous);
        return true;
    }

    private static boolean isUsable(Human human, ItemStack stack) {
        return HumanUtil.isRangedWeapon(stack) && MeleeWeaponSelector.usable(stack)
                && human.hasProjectileForWeapon(stack);
    }

    private static double score(ItemStack stack) {
        int enchantmentLevels = EnchantmentHelper.getEnchantments(stack).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        double durability = stack.getMaxDamage() == 0
                ? 1.0D
                : (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        return enchantmentLevels * 100.0D + durability;
    }

    public record Candidate(int inventorySlot, ItemStack stack, double score) {
        private static final Comparator<Candidate> ORDER = Comparator
                .comparingDouble(Candidate::score).reversed()
                .thenComparingInt(candidate -> candidate.inventorySlot() < 0 ? -1 : candidate.inventorySlot())
                .thenComparing(candidate -> BuiltInRegistries.ITEM.getKey(candidate.stack().getItem()).toString());
    }
}
