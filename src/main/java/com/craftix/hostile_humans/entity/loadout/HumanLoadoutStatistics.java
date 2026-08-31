package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Lightweight deterministic audit utility; it never creates entities. */
public final class HumanLoadoutStatistics {
    private HumanLoadoutStatistics() {}

    public static Statistics sample(SpawnContext context, WorldGearProgressionSnapshot progression,
                                    CombatSkillTier tier, long serverAgeTicks, long seed, int sampleCount) {
        if (sampleCount < 1) throw new IllegalArgumentException("sampleCount must be positive");
        int netherite = 0, shield = 0, bow = 0, enchanted = 0, rareUtility = 0, goldArmor = 0;
        double armorPieces = 0.0D;
        for (int i = 0; i < sampleCount; i++) {
            LoadoutRollContext rollContext = new LoadoutRollContext(context, dimensionFor(context), progression, tier,
                    serverAgeTicks, RandomSource.create(seed + i * 0x9E3779B97F4A7C15L));
            HumanLoadoutDefinition definition = HumanLoadoutGenerator.generate(rollContext);
            if (definition.quality() == HumanLoadoutGenerator.Quality.NETHERITE) netherite++;
            armorPieces += definition.armorPieces();
            if (!definition.equipment(EquipmentSlot.OFFHAND).isEmpty()) shield++;
            if (definition.hasBow()) bow++;
            if (hasEnchantments(definition)) enchanted++;
            if (hasRareUtility(definition)) rareUtility++;
            if (hasGoldArmor(definition)) goldArmor++;
        }
        return new Statistics(sampleCount, (double) netherite / sampleCount, armorPieces / sampleCount,
                (double) shield / sampleCount, (double) bow / sampleCount, (double) enchanted / sampleCount,
                (double) rareUtility / sampleCount, (double) goldArmor / sampleCount);
    }

    private static boolean hasEnchantments(HumanLoadoutDefinition definition) {
        for (EquipmentSlot slot : EquipmentSlot.values()) if (definition.equipment(slot).isEnchanted()) return true;
        return false;
    }

    private static net.minecraft.resources.ResourceKey<Level> dimensionFor(SpawnContext context) {
        if (context == SpawnContext.NETHER_WILDS || context == SpawnContext.NETHER_FORTRESS || context == SpawnContext.BASTION) return Level.NETHER;
        if (context == SpawnContext.END_WILDS || context == SpawnContext.END_CITY) return Level.END;
        return Level.OVERWORLD;
    }

    private static boolean hasRareUtility(HumanLoadoutDefinition definition) {
        return definition.inventory().stream().anyMatch(stack -> stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.TOTEM_OF_UNDYING)
                || stack.is(Items.ENDER_PEARL));
    }

    private static boolean hasGoldArmor(HumanLoadoutDefinition definition) {
        return definition.equipment(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET)
                || definition.equipment(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE)
                || definition.equipment(EquipmentSlot.LEGS).is(Items.GOLDEN_LEGGINGS)
                || definition.equipment(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS);
    }

    public record Statistics(int sampleCount, double netheriteQualityRate, double averageArmorPieces,
                             double shieldRate, double bowRate, double enchantedRate,
                             double rareUtilityRate, double goldArmorRate) {}
}
