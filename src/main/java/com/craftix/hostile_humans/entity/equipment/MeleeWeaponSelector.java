package com.craftix.hostile_humans.entity.equipment;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import com.google.common.collect.Multimap;

import java.util.Comparator;
import java.util.Optional;

/** Deterministic, data-driven melee equipment policy. */
public final class MeleeWeaponSelector {
    public static final TagKey<net.minecraft.world.item.Item> PRIMARY_MELEE_WEAPONS =
            ItemTags.create(new ResourceLocation("hostile_humans", "primary_melee_weapons"));
    public static final TagKey<net.minecraft.world.item.Item> FALLBACK_MELEE_TOOLS =
            ItemTags.create(new ResourceLocation("hostile_humans", "fallback_melee_tools"));
    public static final TagKey<net.minecraft.world.item.Item> NEVER_USE_AS_MELEE_WEAPON =
            ItemTags.create(new ResourceLocation("hostile_humans", "never_use_as_melee_weapon"));

    private MeleeWeaponSelector() {
    }

    public static boolean isPrimary(ItemStack stack) {
        return usable(stack) && stack.is(PRIMARY_MELEE_WEAPONS) && !stack.is(NEVER_USE_AS_MELEE_WEAPON);
    }

    public static boolean isFallback(ItemStack stack) {
        return usable(stack) && stack.is(FALLBACK_MELEE_TOOLS) && !stack.is(NEVER_USE_AS_MELEE_WEAPON);
    }

    public static boolean isMeleeCandidate(ItemStack stack) {
        return isPrimary(stack) || (Config.enableFallbackToolWeapons.get() && isFallback(stack));
    }

    public static boolean usable(ItemStack stack) {
        return !stack.isEmpty() && (stack.getMaxDamage() == 0
                || stack.getDamageValue() < stack.getMaxDamage() - 1);
    }

    public static Optional<Candidate> select(Human human, MobType targetType) {
        Candidate best = null;
        ItemStack mainhand = human.getMainHandItem();
        if (isMeleeCandidate(mainhand)) {
            best = new Candidate(-1, mainhand, score(mainhand, targetType, isPrimary(mainhand)));
        }

        if (human.getData() == null) {
            return Optional.ofNullable(best);
        }
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            ItemStack stack = human.getData().getInventoryItem(slot);
            if (!isMeleeCandidate(stack)) {
                continue;
            }
            Candidate candidate = new Candidate(slot, stack, score(stack, targetType, isPrimary(stack)));
            if (best == null || Candidate.ORDER.compare(candidate, best) < 0) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean equipBest(Human human, MobType targetType) {
        Optional<Candidate> selected = select(human, targetType);
        if (selected.isEmpty() || selected.get().inventorySlot() < 0) {
            return false;
        }

        Candidate candidate = selected.get();
        ItemStack previous = human.getMainHandItem().copy();
        human.setItemSlot(EquipmentSlot.MAINHAND, candidate.stack().copy());
        human.getData().setInventoryItem(candidate.inventorySlot(), previous);
        return true;
    }

    private static double score(ItemStack stack, MobType targetType, boolean primary) {
        double category = primary ? 1_000_000D : 100_000D;
        double damage = effectiveAttackDamage(stack);
        double enchantment = EnchantmentHelper.getDamageBonus(stack, targetType);
        double durability = stack.getMaxDamage() == 0
                ? 1D
                : (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        double nearBreakPenalty = durability < 0.10D ? 250D : 0D;
        return category + damage * 100D + enchantment * 10D + durability * 5D - nearBreakPenalty;
    }

    private static double effectiveAttackDamage(ItemStack stack) {
        double value = Attributes.ATTACK_DAMAGE.getDefaultValue();
        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                value += modifier.getAmount();
            }
        }
        double multiplyBase = 0D;
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                multiplyBase += modifier.getAmount();
            }
        }
        value *= 1D + multiplyBase;
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                value *= 1D + modifier.getAmount();
            }
        }
        return value;
    }

    public record Candidate(int inventorySlot, ItemStack stack, double score) {
        private static final Comparator<Candidate> ORDER = Comparator
                .comparingInt((Candidate candidate) -> candidate.isPrimary() ? 0 : 1)
                .thenComparing(Comparator.comparingDouble(Candidate::score).reversed())
                .thenComparingInt(candidate -> candidate.inventorySlot() < 0 ? -1 : candidate.inventorySlot())
                .thenComparing(candidate -> BuiltInRegistries.ITEM.getKey(candidate.stack().getItem()).toString());

        private boolean isPrimary() {
            return MeleeWeaponSelector.isPrimary(stack);
        }
    }
}
