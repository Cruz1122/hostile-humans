package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Reproducible, entity-free loadout audit used by operators and release tests. */
public final class HumanLoadoutAudit {
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private HumanLoadoutAudit() {
    }

    public static Report sample(SpawnContext context, WorldGearProgressionSnapshot progression,
                                CombatSkillTier tier, long ageTicks, long seed, int samples) {
        if (samples < 1) throw new IllegalArgumentException("samples must be positive");
        int netherite = 0, shield = 0, bow = 0, enchanted = 0, rare = 0, gold = 0;
        int goldenApple = 0, notchApple = 0, totem = 0, pearls = 0, rareArrows = 0;
        double quality = 0.0D, armor = 0.0D, enchantments = 0.0D, enchantPower = 0.0D;
        for (int i = 0; i < samples; i++) {
            HumanLoadoutDefinition definition = HumanLoadoutGenerator.generate(new LoadoutRollContext(
                    context, dimensionFor(context), progression, tier, ageTicks,
                    RandomSource.create(seed + i * SEED_STEP)));
            quality += definition.quality().score();
            armor += definition.armorPieces();
            boolean hasEnchant = false;
            boolean hasGoldArmor = false;
            int definitionEnchantments = 0;
            int definitionEnchantPower = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = definition.equipment(slot);
                if (stack.is(Items.SHIELD)) shield++;
                if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) bow++;
                var stackEnchants = EnchantmentHelper.getEnchantments(stack);
                if (!stackEnchants.isEmpty()) hasEnchant = true;
                definitionEnchantments += stackEnchants.size();
                definitionEnchantPower += stackEnchants.values().stream().mapToInt(Integer::intValue).sum();
                if (isGoldArmor(stack)) hasGoldArmor = true;
            }
            if (hasEnchant) enchanted++;
            if (hasGoldArmor) gold++;
            enchantments += definitionEnchantments;
            enchantPower += definitionEnchantPower;
            for (ItemStack stack : definition.inventory()) {
                if (stack.is(Items.GOLDEN_APPLE)) goldenApple++;
                if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) notchApple++;
                if (stack.is(Items.TOTEM_OF_UNDYING)) totem++;
                if (stack.is(Items.ENDER_PEARL)) pearls++;
                if (stack.getItem() instanceof ArrowItem
                        && (stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW))) rareArrows++;
            }
            if (definition.quality() == HumanLoadoutGenerator.Quality.NETHERITE) netherite++;
            if (definition.inventory().stream().anyMatch(HumanLoadoutAudit::isRareUtility)) rare++;
        }
        return new Report(context, tier, ageTicks, samples,
                rate(netherite, samples), rate(shield, samples), rate(bow, samples),
                rate(enchanted, samples), rate(rare, samples), rate(gold, samples),
                rate(goldenApple, samples), rate(notchApple, samples), rate(totem, samples),
                rate(pearls, samples), rate(rareArrows, samples), quality / samples, armor / samples,
                enchantments / samples, enchantPower / samples);
    }

    public static void writeCsv(Path path, Iterable<Report> reports) throws IOException {
        StringBuilder csv = new StringBuilder("context,tier,age_ticks,samples,netherite_rate,shield_rate,bow_rate,"
                + "enchant_rate,rare_rate,gold_armor_rate,golden_apple_rate,notch_apple_rate,totem_rate,"
                + "pearl_rate,rare_arrow_rate,avg_quality,avg_armor,avg_enchants,avg_enchant_power\n");
        for (Report report : reports) csv.append(report.csvRow()).append('\n');
        Files.createDirectories(path.getParent());
        Files.writeString(path, csv.toString());
    }

    private static double rate(int count, int samples) {
        return (double) count / samples;
    }

    private static boolean isRareUtility(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.ENDER_PEARL);
    }

    private static boolean isGoldArmor(ItemStack stack) {
        return stack.is(Items.GOLDEN_HELMET) || stack.is(Items.GOLDEN_CHESTPLATE)
                || stack.is(Items.GOLDEN_LEGGINGS) || stack.is(Items.GOLDEN_BOOTS);
    }

    private static net.minecraft.resources.ResourceKey<Level> dimensionFor(SpawnContext context) {
        if (context == SpawnContext.NETHER_WILDS || context == SpawnContext.NETHER_FORTRESS
                || context == SpawnContext.BASTION) return Level.NETHER;
        if (context == SpawnContext.END_WILDS || context == SpawnContext.END_CITY) return Level.END;
        return Level.OVERWORLD;
    }

    public record Report(SpawnContext context, CombatSkillTier tier, long ageTicks, int samples,
                         double netheriteRate, double shieldRate, double bowRate, double enchantRate,
                         double rareRate, double goldArmorRate, double goldenAppleRate,
                         double notchAppleRate, double totemRate, double pearlRate, double rareArrowRate,
                         double averageQuality, double averageArmor, double averageEnchants,
                         double averageEnchantPower) {
        public String summary() {
            return String.format(Locale.ROOT,
                    "context=%s tier=%s age=%d samples=%d quality=%.3f armor=%.3f netherite=%.2f%% shield=%.2f%% bow=%.2f%% enchant=%.2f%% rare=%.2f%%",
                    context, tier, ageTicks, samples, averageQuality, averageArmor, netheriteRate * 100.0D,
                    shieldRate * 100.0D, bowRate * 100.0D, enchantRate * 100.0D, rareRate * 100.0D);
        }

        public String csvRow() {
            return String.join(",", context.name(), tier.name(), Long.toString(ageTicks), Integer.toString(samples),
                    format(netheriteRate), format(shieldRate), format(bowRate), format(enchantRate), format(rareRate),
                    format(goldArmorRate), format(goldenAppleRate), format(notchAppleRate), format(totemRate),
                    format(pearlRate), format(rareArrowRate), format(averageQuality), format(averageArmor),
                    format(averageEnchants), format(averageEnchantPower));
        }

        private static String format(double value) {
            return String.format(Locale.ROOT, "%.6f", value);
        }
    }
}
