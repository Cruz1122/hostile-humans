package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;

import java.util.function.Predicate;

public final class GearUpgradePolicy {
    private GearUpgradePolicy() {}

    public static boolean usefulUpgrade(Human human, ItemStack candidate) {
        if (candidate.getItem() instanceof PickaxeItem) return score(candidate) > bestInventoryScore(human, stack -> stack.getItem() instanceof PickaxeItem);
        if (candidate.getItem() instanceof SwordItem) return score(candidate) > bestInventoryScore(human, stack -> stack.getItem() instanceof SwordItem);
        if (candidate.getItem() instanceof AxeItem) return score(candidate) > bestInventoryScore(human, stack -> stack.getItem() instanceof AxeItem);
        if (candidate.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            return score(candidate) > score(human.getItemBySlot(slot));
        }
        return false;
    }

    public static int score(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        if (stack.getItem() instanceof ArmorItem armor) {
            return armor.getDefense() * 1000 + Math.round(armor.getToughness() * 100.0F)
                    + durabilityScore(stack);
        }
        if (stack.getItem() instanceof TieredItem tiered) {
            return tiered.getTier().getLevel() * 10000 + Math.round(tiered.getTier().getSpeed() * 100.0F)
                    + durabilityScore(stack);
        }
        return 0;
    }

    private static int bestInventoryScore(Human human, Predicate<ItemStack> type) {
        int best = type.test(human.getMainHandItem()) ? score(human.getMainHandItem()) : -1;
        if (human.getData() != null) {
            for (ItemStack stack : human.getData().getInventoryItems()) if (type.test(stack)) best = Math.max(best, score(stack));
        }
        return best;
    }

    private static int durabilityScore(ItemStack stack) {
        return stack.getMaxDamage() <= 0 ? 0 : Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) * 10 / stack.getMaxDamage();
    }
}
