package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Calculates the bounded, deterministic XP reward from a Human's live state. */
public final class HumanDeathRewardCalculator {
    private static final int MAX_QUALITY_SCORE = 20;

    private HumanDeathRewardCalculator() {
    }

    public static int calculate(Human human) {
        CombatSkillTier tier = human.getCombatTacticsController().skillTier();
        int base = switch (tier) {
            case T1 -> 30;
            case T2 -> 20;
            case T3 -> 12;
            case T4 -> 8;
            case T5 -> 5;
        };
        int cap = switch (tier) {
            case T1 -> 60;
            case T2 -> 40;
            case T3 -> 25;
            case T4 -> 15;
            case T5 -> 10;
        };
        int quality = qualityScore(human);
        double scale = 1.0D;
        try {
            scale = Config.deathXpScale.get();
        } catch (RuntimeException ignored) {
            // Config is not initialized in some pure GameTest fixtures.
        }
        int reward = base + (int) Math.round((cap - base) * (quality / (double) MAX_QUALITY_SCORE) * scale);
        return Math.max(base, Math.min(cap, reward));
    }

    /** Small score used only for XP and audit output; it is not an economic valuation. */
    public static int qualityScore(Human human) {
        int material = 0;
        int armorPieces = 0;
        int enchantmentLevels = 0;
        boolean shieldOrRanged = false;
        boolean rareUtility = false;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = human.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            material = Math.max(material, materialScore(stack));
            enchantmentLevels = Math.min(6, enchantmentLevels + EnchantmentHelper.getEnchantments(stack).values().stream()
                    .mapToInt(Integer::intValue).sum());
            if (slot.getType() == EquipmentSlot.Type.ARMOR) armorPieces++;
            if (stack.is(Items.SHIELD) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) shieldOrRanged = true;
        }
        if (human.getData() != null) {
            for (ItemStack stack : human.getData().getInventoryItems()) {
                if (stack.isEmpty()) continue;
                material = Math.max(material, materialScore(stack));
                enchantmentLevels = Math.min(6, enchantmentLevels + EnchantmentHelper.getEnchantments(stack).values().stream()
                        .mapToInt(Integer::intValue).sum());
                if (stack.is(Items.SHIELD) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) shieldOrRanged = true;
                if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                        || stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.ENDER_PEARL)) rareUtility = true;
            }
        }

        return Math.min(MAX_QUALITY_SCORE, material + armorPieces + enchantmentLevels
                + (shieldOrRanged ? 2 : 0) + (rareUtility ? 2 : 0));
    }

    private static int materialScore(ItemStack stack) {
        if (stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_AXE)
                || stack.is(Items.NETHERITE_PICKAXE) || stack.getItem() instanceof ArmorItem armor
                && armor.getMaterial().getDefenseForType(armor.getType()) >= 3) return 4;
        if (stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.DIAMOND_PICKAXE)
                || stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
                || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)) return 3;
        if (stack.getItem() instanceof TieredItem tiered && tiered.getTier().getLevel() >= 2) return 2;
        if (stack.is(Items.STONE_SWORD) || stack.is(Items.STONE_AXE) || stack.is(Items.STONE_PICKAXE)
                || stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT)) return 1;
        return 0;
    }
}
