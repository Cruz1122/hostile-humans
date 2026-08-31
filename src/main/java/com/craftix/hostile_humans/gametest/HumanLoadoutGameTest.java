package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutDefinition;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutGenerator;
import com.craftix.hostile_humans.entity.loadout.LoadoutRollContext;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;


@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanLoadoutGameTest {
    private static final String TEMPLATE = "human_smoke";
    private static final int SAMPLES = 10_000;
    private static final WorldGearProgressionSnapshot ALL_UNLOCKED =
            new WorldGearProgressionSnapshot(true, true, true, true, false);
    private static final WorldGearProgressionSnapshot POST_END =
            new WorldGearProgressionSnapshot(true, true, true, true, true);
    private static final TagKey<Item> SPAWN_FOOD = loadoutTag("spawn_food");
    private static final TagKey<Item> HIGH_END_FOOD = loadoutTag("high_end_food");
    private static final TagKey<Item> SPAWN_UTILITY = loadoutTag("spawn_utility");
    private static final TagKey<Item> BASIC_RESOURCES = loadoutTag("resources/basic");
    private static final TagKey<Item> IRON_RESOURCES = loadoutTag("resources/iron");
    private static final TagKey<Item> GOLD_RESOURCES = loadoutTag("resources/gold");
    private static final TagKey<Item> DIAMOND_RESOURCES = loadoutTag("resources/diamond");
    private static final TagKey<Item> NETHERITE_RESOURCES = loadoutTag("resources/netherite");
    private static final TagKey<Item> RARE_ARROWS = loadoutTag("rare_arrows");

    private HumanLoadoutGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void lockedMaterialsNeverAppear(GameTestHelper helper) {
        WorldGearProgressionSnapshot locked = new WorldGearProgressionSnapshot(true, true, false, false, false);
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.END_CITY, locked, CombatSkillTier.T1, 5_184_000L, i);
            helper.assertTrue(!containsAny(definition, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE,
                            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                            Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_PICKAXE,
                            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                            Items.DIAMOND, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS),
                    "Locked material appeared in a loadout");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void bastionAlwaysHasGoldArmor(GameTestHelper helper) {
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.BASTION, ALL_UNLOCKED, CombatSkillTier.T3, 5_184_000L, i);
            helper.assertTrue(hasGoldArmor(definition), "Bastion loadout did not contain gold armor at sample " + i);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void rangedLoadoutsAlwaysHaveArrows(GameTestHelper helper) {
        int ranged = 0;
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.END_WILDS, POST_END, CombatSkillTier.T2, 5_184_000L, i);
            if (definition.hasBow()) {
                ranged++;
                helper.assertTrue(definition.hasAmmo(), "Bow loadout had no usable arrows at sample " + i);
            }
        }
        helper.assertTrue(ranged > SAMPLES / 2, "Expected a substantial ranged population");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void endNetheriteQualityIsDramaticallyHigh(GameTestHelper helper) {
        int netherite = 0;
        for (int i = 0; i < SAMPLES; i++) {
            if (roll(SpawnContext.END_WILDS, ALL_UNLOCKED, CombatSkillTier.T3, 0L, i).quality() == HumanLoadoutGenerator.Quality.NETHERITE) netherite++;
        }
        double rate = (double) netherite / SAMPLES;
        LoadoutRollContext auditContext = new LoadoutRollContext(SpawnContext.END_WILDS, Level.OVERWORLD, ALL_UNLOCKED,
                CombatSkillTier.T3, 0L, RandomSource.create(99L));
        helper.assertTrue(rate >= 0.76D && rate <= 0.88D,
                "End Netherite quality rate was " + rate + ", configured chance=" + HumanLoadoutGenerator.netheriteQualityChance(auditContext));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void postEndDiamondQualityDoesNotProduceLeatherNetheriteSets(GameTestHelper helper) {
        int netheriteSamples = 0;
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.END_WILDS, POST_END, CombatSkillTier.T3, 5_184_000L, i);
            if (definition.quality() != HumanLoadoutGenerator.Quality.NETHERITE) continue;
            netheriteSamples++;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = definition.equipment(slot);
                helper.assertTrue(!isLeather(armor), "Netherite quality produced leather armor");
            }
        }
        helper.assertTrue(netheriteSamples > 2_000, "Post-End Netherite sample was unexpectedly small");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void endAndPostEndHaveAbundantPearlsAndTotems(GameTestHelper helper) {
        int endPearls = 0, endTotems = 0, postEndPearls = 0, postEndTotems = 0;
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition end = roll(SpawnContext.END_WILDS, POST_END, CombatSkillTier.T3, 0L, i);
            HumanLoadoutDefinition postEnd = roll(SpawnContext.OVERWORLD_SURFACE, POST_END, CombatSkillTier.T3, 5_184_000L, i);
            if (containsAny(end, Items.ENDER_PEARL)) endPearls++;
            if (containsAny(end, Items.TOTEM_OF_UNDYING)) endTotems++;
            if (containsAny(postEnd, Items.ENDER_PEARL)) postEndPearls++;
            if (containsAny(postEnd, Items.TOTEM_OF_UNDYING)) postEndTotems++;
        }
        helper.assertTrue(endPearls > 9_500 && endTotems > 9_500,
                "End did not produce abundant pearls/totems: pearls=" + endPearls + ", totems=" + endTotems);
        helper.assertTrue(postEndPearls > 5_500 && postEndTotems > 3_500,
                "Post-End did not produce abundant pearls/totems: pearls=" + postEndPearls + ", totems=" + postEndTotems);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void t1OutperformsT5Statistically(GameTestHelper helper) {
        double t1Quality = 0.0D, t5Quality = 0.0D;
        double t1Armor = 0.0D, t5Armor = 0.0D;
        int t1Enchanted = 0, t5Enchanted = 0;
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition t1 = roll(SpawnContext.OVERWORLD_SURFACE, ALL_UNLOCKED, CombatSkillTier.T1, 5_184_000L, i);
            HumanLoadoutDefinition t5 = roll(SpawnContext.OVERWORLD_SURFACE, ALL_UNLOCKED, CombatSkillTier.T5, 5_184_000L, i);
            t1Quality += t1.quality().score();
            t5Quality += t5.quality().score();
            t1Armor += t1.armorPieces();
            t5Armor += t5.armorPieces();
            if (hasEnchant(t1)) t1Enchanted++;
            if (hasEnchant(t5)) t5Enchanted++;
        }
        helper.assertTrue(t1Quality > t5Quality, "T1 quality did not exceed T5");
        helper.assertTrue(t1Armor > t5Armor, "T1 armor completeness did not exceed T5");
        helper.assertTrue(t1Enchanted > t5Enchanted, "T1 enchantment rate did not exceed T5");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void serverAgeDoesNotUnlockMaterials(GameTestHelper helper) {
        WorldGearProgressionSnapshot locked = new WorldGearProgressionSnapshot(true, true, false, false, true);
        for (int i = 0; i < SAMPLES; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.OVERWORLD_SURFACE, locked, CombatSkillTier.T1, Long.MAX_VALUE, i);
            helper.assertTrue(!containsAny(definition, Items.DIAMOND, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS),
                    "Server age bypassed progression cap");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void inventoryCandidatesComeFromLoadoutTags(GameTestHelper helper) {
        for (int i = 0; i < 2_000; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.END_WILDS, POST_END, CombatSkillTier.T1, 5_184_000L, i);
            for (ItemStack stack : definition.inventory()) {
                helper.assertTrue(isLoadoutCandidate(stack),
                        "Inventory item was not supplied by a declared loadout source: " + stack);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void netherAndEndContextsAlwaysUseFullArmor(GameTestHelper helper) {
        SpawnContext[] contexts = {SpawnContext.NETHER_WILDS, SpawnContext.NETHER_FORTRESS,
                SpawnContext.BASTION, SpawnContext.END_WILDS, SpawnContext.END_CITY};
        for (SpawnContext context : contexts) {
            for (int i = 0; i < 500; i++) {
                HumanLoadoutDefinition definition = roll(context, POST_END, CombatSkillTier.T3, 5_184_000L, i);
                helper.assertTrue(definition.armorPieces() == 4,
                        "" + context + " produced incomplete armor at sample " + i);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void postEndOverworldAlwaysUsesFullArmor(GameTestHelper helper) {
        for (int i = 0; i < 500; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.OVERWORLD_SURFACE, POST_END,
                    CombatSkillTier.T3, 5_184_000L, i);
            helper.assertTrue(definition.armorPieces() == 4,
                    "Post-End Overworld produced incomplete armor at sample " + i);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void netherAndEndContextsFillArmorWithPartialProgression(GameTestHelper helper) {
        WorldGearProgressionSnapshot partial = new WorldGearProgressionSnapshot(true, true, false, false, false);
        for (SpawnContext context : new SpawnContext[]{SpawnContext.NETHER_WILDS, SpawnContext.END_WILDS}) {
            for (int i = 0; i < 500; i++) {
                HumanLoadoutDefinition definition = roll(context, partial, CombatSkillTier.T3, 5_184_000L, i);
                helper.assertTrue(definition.armorPieces() == 4,
                        "" + context + " left an armor slot empty under partial progression at sample " + i);
                helper.assertTrue(!containsAny(definition, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                                Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.NETHERITE_HELMET,
                                Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS),
                        "" + context + " bypassed a locked armor material");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanLoadouts", timeoutTicks = 40)
    public static void enchantmentsAreCommonAndHigherLevelsAreRarer(GameTestHelper helper) {
        int enchantedLoadouts = 0;
        int totalEnchantmentLevels = 0;
        int level2OrHigher = 0, level3OrHigher = 0, level4OrHigher = 0, level5 = 0;
        for (int i = 0; i < 2_000; i++) {
            HumanLoadoutDefinition definition = roll(SpawnContext.END_CITY, POST_END, CombatSkillTier.T1, 5_184_000L, i);
            boolean enchanted = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                for (int level : EnchantmentHelper.getEnchantments(definition.equipment(slot)).values()) {
                    enchanted = true;
                    totalEnchantmentLevels++;
                    if (level >= 2) level2OrHigher++;
                    if (level >= 3) level3OrHigher++;
                    if (level >= 4) level4OrHigher++;
                    if (level >= 5) level5++;
                }
            }
            if (enchanted) enchantedLoadouts++;
        }
        helper.assertTrue(enchantedLoadouts >= 1_500,
                "Enchantments were not common enough: " + enchantedLoadouts + "/2000 loadouts");
        helper.assertTrue(totalEnchantmentLevels > level2OrHigher && level2OrHigher > level3OrHigher
                        && level3OrHigher > level4OrHigher && level4OrHigher >= level5,
                "Higher enchantment levels did not become progressively rarer: total=" + totalEnchantmentLevels
                        + ", level2+=" + level2OrHigher + ", level3+=" + level3OrHigher
                        + ", level4+=" + level4OrHigher + ", level5=" + level5);
        helper.succeed();
    }

    private static HumanLoadoutDefinition roll(SpawnContext context, WorldGearProgressionSnapshot progression,
                                                CombatSkillTier tier, long age, long seed) {
        return HumanLoadoutGenerator.generate(new LoadoutRollContext(context, Level.OVERWORLD, progression,
                tier, age, RandomSource.create(0x504832L + seed * 0x9E3779B97F4A7C15L)));
    }

    private static boolean hasGoldArmor(HumanLoadoutDefinition definition) {
        return definition.equipment(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET)
                || definition.equipment(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE)
                || definition.equipment(EquipmentSlot.LEGS).is(Items.GOLDEN_LEGGINGS)
                || definition.equipment(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS);
    }

    private static boolean hasEnchant(HumanLoadoutDefinition definition) {
        for (EquipmentSlot slot : EquipmentSlot.values()) if (definition.equipment(slot).isEnchanted()) return true;
        return false;
    }

    private static boolean isLeather(ItemStack stack) {
        return stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS);
    }

    private static boolean containsAny(HumanLoadoutDefinition definition, Item... items) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (Item item : items) if (definition.equipment(slot).is(item)) return true;
        }
        for (ItemStack stack : definition.inventory()) for (Item item : items) if (stack.is(item)) return true;
        return false;
    }

    private static boolean isLoadoutCandidate(ItemStack stack) {
        return stack.getItem() instanceof ArrowItem || isConfiguredCandidate(stack)
                || isLoadoutTool(stack)
                || stack.is(SPAWN_FOOD) || stack.is(HIGH_END_FOOD) || stack.is(SPAWN_UTILITY)
                || stack.is(BASIC_RESOURCES) || stack.is(IRON_RESOURCES) || stack.is(GOLD_RESOURCES)
                || stack.is(DIAMOND_RESOURCES) || stack.is(NETHERITE_RESOURCES) || stack.is(RARE_ARROWS);
    }

    private static boolean isConfiguredCandidate(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD) || stack.is(Items.IRON_SWORD)
                || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.SHIELD)
                || stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.TRIDENT);
    }

    private static boolean isLoadoutTool(ItemStack stack) {
        return stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE) || stack.is(Items.IRON_AXE)
                || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)
                || stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) || stack.is(Items.IRON_PICKAXE)
                || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
    }

    private static TagKey<Item> loadoutTag(String path) {
        return ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/" + path));
    }
}
