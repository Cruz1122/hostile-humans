package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.HumanTier;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One context-driven generator for every naturally spawned Human. */
public final class HumanLoadoutGenerator {
    private static final int DEFAULT_AGE_CAP = 5_184_000;
    private static final TagKey<Item> SPAWN_FOOD = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/spawn_food"));
    private static final TagKey<Item> HIGH_END_FOOD = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/high_end_food"));
    private static final TagKey<Item> SPAWN_UTILITY = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/spawn_utility"));
    private static final TagKey<Item> BASIC_RESOURCES = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/resources/basic"));
    private static final TagKey<Item> IRON_RESOURCES = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/resources/iron"));
    private static final TagKey<Item> GOLD_RESOURCES = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/resources/gold"));
    private static final TagKey<Item> DIAMOND_RESOURCES = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/resources/diamond"));
    private static final TagKey<Item> NETHERITE_RESOURCES = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/resources/netherite"));
    private static final TagKey<Item> RARE_ARROWS = ItemTags.create(new ResourceLocation("hostile_humans", "human_loadout/rare_arrows"));

    private HumanLoadoutGenerator() {}

    public enum Quality {
        SCRAPPY(0), IRON(1), DIAMOND(2), NETHERITE(3);
        private final int score;
        Quality(int score) { this.score = score; }
        public int score() { return score; }
    }

    /** Pure with respect to entities/worlds: only registries, stacks and the supplied RNG are used. */
    public static HumanLoadoutDefinition generate(LoadoutRollContext context) {
        return generate(context, false);
    }

    /** Generates a fully random roll, optionally using the enchanted-egg power ceiling. */
    public static HumanLoadoutDefinition generate(LoadoutRollContext context, boolean enchantedEgg) {
        RandomSource random = context.random();
        Quality quality = enchantedEgg ? maximumQualityFor(context.tier()) : rollQuality(context);
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        List<ItemStack> inventory = new ArrayList<>();

        boolean ranged = random.nextDouble() < rangedChance(context);
        ItemStack weapon = ranged ? stack(Items.BOW) : stack(random.nextDouble() < 0.22 ? axeFor(quality, context) : swordFor(quality, context));
        equipment.put(EquipmentSlot.MAINHAND, damage(weapon, random, context));

        if (random.nextDouble() < shieldChance(context)) equipment.put(EquipmentSlot.OFFHAND, damage(stack(Items.SHIELD), random, context));

        int armorPieces = armorPieceCount(context);
        List<EquipmentSlot> armorSlots = new ArrayList<>(List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET));
        for (int i = 0; i < armorPieces; i++) {
            EquipmentSlot slot = armorSlots.remove(random.nextInt(armorSlots.size()));
            equipment.put(slot, damage(armorFor(slot, quality, context, random), random, context));
        }
        ensureBastionGold(equipment, quality, context, random);

        if (random.nextDouble() < pickaxeChance(context)) add(inventory, damage(stack(pickaxeFor(quality, context)), random, context));
        if (random.nextDouble() < axeChance(context)) add(inventory, damage(stack(axeFor(quality, context)), random, context));
        if (ranged) add(inventory, arrows(context, random));

        int foodCount = foodCount(context);
        add(inventory, new ItemStack(foodFor(context, random), foodCount));
        addCombatPotions(inventory, context, random);
        if (random.nextDouble() < utilityChance(context)) addTaggedFallback(inventory, Items.COBWEB, SPAWN_UTILITY, 1 + random.nextInt(3), random);
        if (random.nextDouble() < pearlChance(context)) addTaggedFallback(inventory, Items.ENDER_PEARL, SPAWN_UTILITY, 1 + random.nextInt(4), random);
        if (random.nextDouble() < waterBucketChance(context)) addTaggedFallback(inventory, Items.WATER_BUCKET, SPAWN_UTILITY, 1, random);
        if (random.nextDouble() < goldenAppleChance(context)) addTaggedFallback(inventory, Items.GOLDEN_APPLE, SPAWN_UTILITY, 1, random);
        if (random.nextDouble() < notchAppleChance(context)) addTaggedFallback(inventory, Items.ENCHANTED_GOLDEN_APPLE, SPAWN_UTILITY, 1, random);
        if (random.nextDouble() < totemChance(context)) addTaggedFallback(inventory, Items.TOTEM_OF_UNDYING, SPAWN_UTILITY, 1, random);

        addResources(inventory, quality, context, random);
        applyConfiguredNaturalCandidates(equipment, inventory, context, random);
        enchant(equipment, quality, context, enchantedEgg);
        validate(equipment, inventory, quality, context, random);
        return new HumanLoadoutDefinition(quality, equipment, inventory);
    }

    private static Quality maximumQualityFor(CombatSkillTier tier) {
        return switch (tier) {
            case T1 -> Quality.NETHERITE;
            case T2 -> Quality.DIAMOND;
            case T3, T4 -> Quality.IRON;
            case T5 -> Quality.SCRAPPY;
        };
    }

    /** Integration entry point; the natural lifecycle supplies the real entity only at apply time. */
    public static void generateAndApply(ServerLevel level, Human human) {
        generateAndApply(level, human, WorldGearProgressionSnapshot.from(com.craftix.hostile_humans.progression.WorldGearProgressionSavedData.get(level)),
                level.getServer().overworld().getGameTime());
    }

    /** Applies an enchanted egg roll without inheriting the world's progression locks. */
    public static void generateAndApply(ServerLevel level, Human human, boolean enchantedEgg) {
        if (!enchantedEgg) {
            generateAndApply(level, human);
            return;
        }
        LoadoutRollContext context = new LoadoutRollContext(human.getSpawnContext(), level.dimension(),
                new WorldGearProgressionSnapshot(true, true, true, true, true),
                human.getCombatTacticsController().skillTier(), DEFAULT_AGE_CAP, human.getRandom());
        HumanLoadoutDefinition definition = generate(context, true);
        definition.applyTo(human);
    }

    /** Demonstration/debug entry point that can supply a synthetic progression state. */
    public static void generateAndApply(ServerLevel level, Human human,
                                        WorldGearProgressionSnapshot progression, long serverAgeTicks) {
        LoadoutRollContext context = new LoadoutRollContext(human.getSpawnContext(), level.dimension(), progression,
                human.getCombatTacticsController().skillTier(), serverAgeTicks, human.getRandom());
        HumanLoadoutDefinition definition = generate(context);
        definition.applyTo(human);
    }

    public static double ageFactor(long gameTime) {
        int cap = DEFAULT_AGE_CAP;
        try { cap = Math.max(1, Config.loadoutAgeCapTicks.get()); } catch (RuntimeException ignored) {}
        return clamp((double) Math.max(0L, gameTime) / cap);
    }

    public static double netheriteQualityChance(LoadoutRollContext context) {
        return netheriteChance(context);
    }

    /** Repairs missing showcase armor after entity registration/data synchronization. */
    public static void ensureFullArmorForDebug(ServerLevel level, Human human,
                                               WorldGearProgressionSnapshot progression, long serverAgeTicks) {
        LoadoutRollContext context = new LoadoutRollContext(human.getSpawnContext(), level.dimension(), progression,
                human.getCombatTacticsController().skillTier(), serverAgeTicks, human.getRandom());
        if (!requiresFullArmor(context)) return;

        Quality quality = qualityFromExistingEquipment(human);
        EnumMap<EquipmentSlot, ItemStack> missing = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : armorSlots()) {
            if (human.getItemBySlot(slot).isEmpty()) {
                missing.put(slot, damage(armorFor(slot, quality, context, context.random()), context.random(), context));
            }
        }
        enchant(missing, quality, context);
        missing.forEach(human::setItemSlot);
    }

    private static Quality rollQuality(LoadoutRollContext context) {
        RandomSource random = context.random();
        double netherite = netheriteChance(context);
        if (context.progression().netheriteUnlocked() && random.nextDouble() < netherite) return Quality.NETHERITE;

        boolean advanced = context.progression().diamondUnlocked();
        double diamond = diamondChance(context);
        if (advanced && random.nextDouble() < diamond) return Quality.DIAMOND;

        if (context.progression().ironUnlocked() && random.nextDouble() < ironChance(context)) return Quality.IRON;
        return Quality.SCRAPPY;
    }

    private static double netheriteChance(LoadoutRollContext context) {
        if (!context.progression().netheriteUnlocked()) return 0.0D;
        SpawnContext spawn = context.spawnContext();
        double base;
        if (isEnd(spawn)) base = Config.loadoutEndNetheriteChance.get();
        else if (isNether(spawn)) base = context.progression().endVisited()
                ? Config.loadoutPostEndNetherNetheriteChance.get() : Config.loadoutNetherNetheriteChance.get();
        else base = context.progression().endVisited()
                ? Config.loadoutPostEndOverworldNetheriteChance.get() : Config.loadoutOverworldNetheriteChance.get();

        if (spawn == SpawnContext.NETHER_FORTRESS) base += 0.03D;
        if (spawn == SpawnContext.BASTION) base += 0.07D;
        if (spawn == SpawnContext.END_CITY) base += 0.05D;
        base *= 0.80D + tierStrength(context.tier()) * 0.40D;
        base *= 1.0D + context.ageFactor() * 0.20D;
        double cap = isEnd(spawn) ? Config.loadoutEndNetheriteCap.get()
                : isNether(spawn) ? Config.loadoutNetherNetheriteCap.get() : Config.loadoutOverworldNetheriteCap.get();
        return clamp(Math.min(base, cap));
    }

    private static double diamondChance(LoadoutRollContext context) {
        SpawnContext spawn = context.spawnContext();
        double base = isEnd(spawn) ? Config.loadoutDiamondEndChance.get()
                : isNether(spawn) ? Config.loadoutDiamondNetherChance.get() : Config.loadoutDiamondOverworldChance.get();
        if (context.progression().endVisited()) base += isEnd(spawn) ? 0.10D : Config.loadoutPostEndDiamondBoost.get();
        if (spawn == SpawnContext.NETHER_FORTRESS || spawn == SpawnContext.BASTION) base += 0.12D;
        if (spawn == SpawnContext.END_CITY) base += 0.08D;
        return clamp(base + tierStrength(context.tier()) * 0.15D + context.ageFactor() * 0.12D);
    }

    private static double ironChance(LoadoutRollContext context) {
        return clamp((isNether(context.spawnContext()) || isEnd(context.spawnContext()) ? 0.82D : 0.64D)
                + tierStrength(context.tier()) * 0.12D);
    }

    private static int armorPieceCount(LoadoutRollContext context) {
        if (requiresFullArmor(context)) return 4;
        int tier = context.tier().ordinal();
        int min = switch (tier) { case 0, 1 -> 3; case 2 -> 2; case 3 -> 1; default -> 0; };
        int max = switch (tier) { case 0, 1, 2 -> 4; case 3 -> 3; default -> 2; };
        if (context.spawnContext() == SpawnContext.BASTION || context.spawnContext() == SpawnContext.END_CITY) min++;
        if (context.ageFactor() > 0.55D) min++;
        min = Math.min(4, min);
        max = Math.min(4, Math.max(min, max));
        return min + context.random().nextInt(max - min + 1);
    }

    private static void ensureBastionGold(EnumMap<EquipmentSlot, ItemStack> equipment, Quality quality,
                                          LoadoutRollContext context, RandomSource random) {
        if (context.spawnContext() != SpawnContext.BASTION || !context.progression().goldUnlocked()) return;
        EquipmentSlot slot = switch (random.nextInt(4)) {
            case 0 -> EquipmentSlot.HEAD; case 1 -> EquipmentSlot.CHEST; case 2 -> EquipmentSlot.LEGS; default -> EquipmentSlot.FEET;
        };
        equipment.put(slot, damage(stack(armorItem(ArmorMaterials.GOLD, slot)), random, context));
    }

    private static ItemStack armorFor(EquipmentSlot slot, Quality quality, LoadoutRollContext context, RandomSource random) {
        int maximum = quality == Quality.NETHERITE ? 4 : quality == Quality.DIAMOND ? 3
                : quality == Quality.IRON ? 2 : 1;
        if (!context.progression().netheriteUnlocked()) maximum = Math.min(maximum, 3);
        if (!context.progression().diamondUnlocked()) maximum = Math.min(maximum, 2);
        int selected = Math.max(0, maximum - random.nextInt(Math.max(1, quality.score() >= 2 ? 2 : 3)));
        ArmorMaterial material = switch (selected) {
            case 4 -> ArmorMaterials.NETHERITE;
            case 3 -> ArmorMaterials.DIAMOND;
            case 2 -> context.progression().ironUnlocked() ? ArmorMaterials.IRON : ArmorMaterials.CHAIN;
            default -> ArmorMaterials.LEATHER;
        };
        if (quality.score() >= 2 && material == ArmorMaterials.LEATHER) material = ArmorMaterials.IRON;
        if (quality == Quality.NETHERITE && material == ArmorMaterials.IRON && context.progression().diamondUnlocked()) material = ArmorMaterials.DIAMOND;
        return stack(armorItem(material, slot));
    }

    private static Item armorItem(ArmorMaterial material, EquipmentSlot slot) {
        boolean head = slot == EquipmentSlot.HEAD, chest = slot == EquipmentSlot.CHEST, legs = slot == EquipmentSlot.LEGS;
        if (material == ArmorMaterials.NETHERITE) return head ? Items.NETHERITE_HELMET : chest ? Items.NETHERITE_CHESTPLATE : legs ? Items.NETHERITE_LEGGINGS : Items.NETHERITE_BOOTS;
        if (material == ArmorMaterials.DIAMOND) return head ? Items.DIAMOND_HELMET : chest ? Items.DIAMOND_CHESTPLATE : legs ? Items.DIAMOND_LEGGINGS : Items.DIAMOND_BOOTS;
        if (material == ArmorMaterials.IRON) return head ? Items.IRON_HELMET : chest ? Items.IRON_CHESTPLATE : legs ? Items.IRON_LEGGINGS : Items.IRON_BOOTS;
        if (material == ArmorMaterials.GOLD) return head ? Items.GOLDEN_HELMET : chest ? Items.GOLDEN_CHESTPLATE : legs ? Items.GOLDEN_LEGGINGS : Items.GOLDEN_BOOTS;
        if (material == ArmorMaterials.CHAIN) return head ? Items.CHAINMAIL_HELMET : chest ? Items.CHAINMAIL_CHESTPLATE : legs ? Items.CHAINMAIL_LEGGINGS : Items.CHAINMAIL_BOOTS;
        return head ? Items.LEATHER_HELMET : chest ? Items.LEATHER_CHESTPLATE : legs ? Items.LEATHER_LEGGINGS : Items.LEATHER_BOOTS;
    }

    private static Item swordFor(Quality quality, LoadoutRollContext context) {
        if (quality == Quality.NETHERITE && context.progression().netheriteUnlocked()) return Items.NETHERITE_SWORD;
        if (quality == Quality.DIAMOND && context.progression().diamondUnlocked()) return Items.DIAMOND_SWORD;
        if (quality == Quality.IRON && context.progression().ironUnlocked()) return Items.IRON_SWORD;
        return context.random().nextBoolean() ? Items.STONE_SWORD : Items.WOODEN_SWORD;
    }

    private static Item axeFor(Quality quality, LoadoutRollContext context) {
        if (quality == Quality.NETHERITE && context.progression().netheriteUnlocked()) return Items.NETHERITE_AXE;
        if (quality == Quality.DIAMOND && context.progression().diamondUnlocked()) return Items.DIAMOND_AXE;
        if (quality == Quality.IRON && context.progression().ironUnlocked()) return Items.IRON_AXE;
        return context.random().nextBoolean() ? Items.STONE_AXE : Items.WOODEN_AXE;
    }

    private static Item pickaxeFor(Quality quality, LoadoutRollContext context) {
        if (quality == Quality.NETHERITE && context.progression().netheriteUnlocked()) return Items.NETHERITE_PICKAXE;
        if (quality == Quality.DIAMOND && context.progression().diamondUnlocked()) return Items.DIAMOND_PICKAXE;
        if (quality == Quality.IRON && context.progression().ironUnlocked()) return Items.IRON_PICKAXE;
        return context.random().nextBoolean() ? Items.STONE_PICKAXE : Items.WOODEN_PICKAXE;
    }

    private static void addResources(List<ItemStack> inventory, Quality quality, LoadoutRollContext context, RandomSource random) {
        addTaggedFallback(inventory, Items.COAL, BASIC_RESOURCES, 2 + random.nextInt(7), random);
        if (context.progression().ironUnlocked() && random.nextDouble() < 0.50D) addTaggedFallback(inventory, Items.IRON_INGOT, IRON_RESOURCES, 1 + random.nextInt(5), random);
        if (context.progression().goldUnlocked() && random.nextDouble() < (isNether(context.spawnContext()) ? 0.45D : 0.12D)) addTaggedFallback(inventory, Items.GOLD_INGOT, GOLD_RESOURCES, 1 + random.nextInt(4), random);
        if (context.progression().diamondUnlocked() && random.nextDouble() < (quality.score() >= 2 ? 0.30D : 0.08D)) addTaggedFallback(inventory, Items.DIAMOND, DIAMOND_RESOURCES, 1 + random.nextInt(3), random);
        if (context.progression().netheriteUnlocked() && random.nextDouble() < (isEnd(context.spawnContext()) ? 0.08D : isNether(context.spawnContext()) ? 0.025D : 0.004D)) {
            addTaggedFallback(inventory, random.nextBoolean() ? Items.NETHERITE_SCRAP : Items.ANCIENT_DEBRIS, NETHERITE_RESOURCES, 1, random);
        }
    }

    /**
     * Keeps the established JSON contract useful for natural spawns without
     * allowing its old material lists to bypass progression validation. The
     * procedural roll remains authoritative for progression and context, while
     * configured pools can contribute modded or pack-specific candidates.
     */
    private static void applyConfiguredNaturalCandidates(EnumMap<EquipmentSlot, ItemStack> equipment,
                                                         List<ItemStack> inventory, LoadoutRollContext context,
                                                         RandomSource random) {
        HumanLoadoutManager.HumanLoadout configured = HumanLoadoutManager.get(HumanTier.ROAMER);
        if (configured == null) return;

        if (random.nextDouble() < 0.35D) {
            boolean ranged = equipment.getOrDefault(EquipmentSlot.MAINHAND, ItemStack.EMPTY).getItem()
                    instanceof net.minecraft.world.item.ProjectileWeaponItem;
            HumanLoadoutManager.ItemEntry entry = ranged && !configured.rangedMainhand.isEmpty()
                    ? configured.rangedMainhand.roll(random) : configured.mainhand.roll(random);
            Item item = configuredItem(entry);
            if (item != null) equipment.put(EquipmentSlot.MAINHAND, damage(stack(item), random, context));
        }

        if (random.nextDouble() < configured.offhand.chance) {
            Item item = configuredItem(configured.offhand.roll(random));
            if (item != null) equipment.put(EquipmentSlot.OFFHAND, damage(stack(item), random, context));
        }

        if (random.nextDouble() < 0.35D) {
            Item item = configuredItem(configured.inventory.roll(random));
            if (item != null) add(inventory, damage(stack(item), random, context));
        }

        if (random.nextDouble() < configured.bonusMainhand.chance) {
            Item item = configuredItem(configured.bonusMainhand.roll(random));
            if (item != null) equipment.put(EquipmentSlot.MAINHAND, damage(stack(item), random, context));
        }

        HumanLoadoutManager.ArmorSetEntry armorSet = configured.armorSets.roll(random);
        if (armorSet != null && random.nextDouble() < 0.25D) {
            putConfiguredArmor(equipment, EquipmentSlot.HEAD, armorSet.head, context, random);
            putConfiguredArmor(equipment, EquipmentSlot.CHEST, armorSet.chest, context, random);
            putConfiguredArmor(equipment, EquipmentSlot.LEGS, armorSet.legs, context, random);
            putConfiguredArmor(equipment, EquipmentSlot.FEET, armorSet.feet, context, random);
        }
    }

    private static void putConfiguredArmor(EnumMap<EquipmentSlot, ItemStack> equipment, EquipmentSlot slot,
                                           ResourceLocation itemId, LoadoutRollContext context, RandomSource random) {
        Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
        if (item != null && item != Items.AIR) equipment.put(slot, damage(stack(item), random, context));
    }

    private static Item configuredItem(HumanLoadoutManager.ItemEntry entry) {
        if (entry == null || !entry.isAvailable()) return null;
        Item item = ForgeRegistries.ITEMS.getValue(entry.itemId);
        return item == Items.AIR ? null : item;
    }

    private static ItemStack arrows(LoadoutRollContext context, RandomSource random) {
        int count = arrowCount(context);
        double specialChance = context.tier().ordinal() <= 1 && (isNether(context.spawnContext()) || isEnd(context.spawnContext())) ? 0.10D : 0.025D;
        if (random.nextDouble() >= specialChance) return new ItemStack(Items.ARROW, count);
        Item rareArrow = randomTaggedItem(RARE_ARROWS, random);
        if (rareArrow == Items.TIPPED_ARROW) return PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, Math.max(1, count / 4)), Potions.POISON);
        if (rareArrow instanceof net.minecraft.world.item.ArrowItem) return new ItemStack(rareArrow, Math.max(1, count / 4));
        return new ItemStack(Items.ARROW, count);
    }

    private static void enchant(EnumMap<EquipmentSlot, ItemStack> equipment, Quality quality,
                                 LoadoutRollContext context) {
        enchant(equipment, quality, context, false);
    }

    private static void enchant(EnumMap<EquipmentSlot, ItemStack> equipment, Quality quality,
                                LoadoutRollContext context, boolean enchantedEgg) {
        double chance = switch (context.tier()) {
            case T1 -> Config.loadoutEnchantChanceT1.get();
            case T2 -> Config.loadoutEnchantChanceT2.get();
            case T3 -> Config.loadoutEnchantChanceT3.get();
            case T4 -> Config.loadoutEnchantChanceT4.get();
            case T5 -> Config.loadoutEnchantChanceT5.get();
        };
        chance += quality.score() * 0.02D + context.ageFactor() * 0.08D;
        if (isNether(context.spawnContext()) || isEnd(context.spawnContext())) chance += 0.08D;
        chance = enchantedEgg ? 1.0D : clamp(chance);
        int power = switch (context.tier()) { case T1 -> 28; case T2 -> 22; case T3 -> 16; case T4 -> 11; case T5 -> 7; };
        power += (int) (context.ageFactor() * 8.0D);
        if (enchantedEgg) power += 15;
        boolean treasure = context.random().nextDouble() < 0.025D + context.ageFactor() * 0.025D;
        for (ItemStack stack : equipment.values()) {
            if (stack.isEmpty() || !stack.isEnchantable() || context.random().nextDouble() >= chance) continue;
            List<EnchantmentInstance> possible = EnchantmentHelper.selectEnchantment(context.random(), stack, power, treasure);
            Map<Enchantment, Integer> selected = new java.util.HashMap<>();
            for (EnchantmentInstance candidate : possible) {
                if (!candidate.enchantment.isCurse() && candidate.enchantment.canEnchant(stack)
                        && context.random().nextDouble() < retentionChance(candidate.level)
                        && EnchantmentHelper.isEnchantmentCompatible(selected.keySet(), candidate.enchantment)) {
                    selected.put(candidate.enchantment, candidate.level);
                }
            }
            EnchantmentHelper.setEnchantments(selected, stack);
        }
    }

    private static double retentionChance(int level) {
        return switch (level) {
            case 1 -> 1.0D;
            case 2 -> Config.loadoutEnchantLevel2Chance.get();
            case 3 -> Config.loadoutEnchantLevel3Chance.get();
            case 4 -> Config.loadoutEnchantLevel4Chance.get();
            default -> Config.loadoutEnchantLevel5Chance.get();
        };
    }

    private static void validate(EnumMap<EquipmentSlot, ItemStack> equipment, List<ItemStack> inventory,
                                 Quality quality, LoadoutRollContext context, RandomSource random) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = equipment.get(slot);
            if (stack != null && !stack.isEmpty() && exceedsProgression(stack, context.progression())) equipment.remove(slot);
        }
        inventory.removeIf(stack -> exceedsProgression(stack, context.progression()));
        inventory.removeIf(ItemStack::isEmpty);
        while (inventory.size() > 36) inventory.remove(inventory.size() - 1);

        ItemStack mainhand = equipment.getOrDefault(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (mainhand.isEmpty()) equipment.put(EquipmentSlot.MAINHAND, damage(stack(swordFor(quality, context)), random, context));
        if (equipment.get(EquipmentSlot.MAINHAND).getItem() instanceof net.minecraft.world.item.BowItem
                && inventory.stream().noneMatch(stack -> stack.getItem() instanceof net.minecraft.world.item.ArrowItem)) {
            if (inventory.size() < 36) inventory.add(new ItemStack(Items.ARROW, 8));
            else inventory.set(35, new ItemStack(Items.ARROW, 8));
        }
        ensureFullArmor(equipment, quality, context, random);
        ensureBastionGold(equipment, quality, context, random);
    }

    private static void ensureFullArmor(EnumMap<EquipmentSlot, ItemStack> equipment, Quality quality,
                                         LoadoutRollContext context, RandomSource random) {
        if (!requiresFullArmor(context)) return;
        for (EquipmentSlot slot : armorSlots()) {
            if (equipment.getOrDefault(slot, ItemStack.EMPTY).isEmpty()) {
                equipment.put(slot, damage(armorFor(slot, quality, context, random), random, context));
            }
        }
    }

    private static boolean requiresFullArmor(LoadoutRollContext context) {
        return isNether(context.spawnContext()) || isEnd(context.spawnContext())
                || context.progression().endVisited();
    }

    private static EquipmentSlot[] armorSlots() {
        return new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }

    private static Quality qualityFromExistingEquipment(Human human) {
        for (EquipmentSlot slot : armorSlots()) {
            Item item = human.getItemBySlot(slot).getItem();
            if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
                    || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) return Quality.NETHERITE;
        }
        for (EquipmentSlot slot : armorSlots()) {
            Item item = human.getItemBySlot(slot).getItem();
            if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
                    || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) return Quality.DIAMOND;
        }
        for (EquipmentSlot slot : armorSlots()) {
            Item item = human.getItemBySlot(slot).getItem();
            if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE
                    || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) return Quality.IRON;
        }
        return Quality.SCRAPPY;
    }

    private static boolean exceedsProgression(ItemStack stack, com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot progression) {
        if (stack.is(Items.DIAMOND) || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.DIAMOND_HELMET)
                || stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)) {
            return !progression.diamondUnlocked();
        }
        if (stack.is(Items.NETHERITE_INGOT) || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.ANCIENT_DEBRIS)
                || stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_AXE) || stack.is(Items.NETHERITE_PICKAXE)
                || stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
                || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)) {
            return !progression.netheriteUnlocked();
        }
        if (stack.is(Items.IRON_INGOT) || stack.is(Items.IRON_SWORD) || stack.is(Items.IRON_AXE)
                || stack.is(Items.IRON_PICKAXE) || stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
                || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)) return !progression.ironUnlocked();
        return false;
    }

    private static Item foodFor(LoadoutRollContext context, RandomSource random) {
        if (context.tier().ordinal() <= 1 && random.nextDouble() < 0.22D) {
            Item highEndFood = randomTaggedItem(HIGH_END_FOOD, random);
            if (highEndFood != null) return highEndFood;
        }
        Item candidate = randomTaggedItem(SPAWN_FOOD, random);
        return candidate == null ? Items.COOKED_BEEF : candidate;
    }

    private static int foodCount(LoadoutRollContext c) { return 8 + (int) (tierStrength(c.tier()) * 12) + c.random().nextInt(5); }
    private static void addCombatPotions(List<ItemStack> inventory, LoadoutRollContext c, RandomSource random) {
        if (random.nextDouble() < clamp(0.18D + tierStrength(c.tier()) * 0.20D)) {
            add(inventory, PotionUtils.setPotion(new ItemStack(Items.POTION),
                    random.nextBoolean() ? Potions.SWIFTNESS : Potions.STRENGTH));
        }
        if (random.nextDouble() < clamp(0.12D + tierStrength(c.tier()) * 0.16D)) {
            add(inventory, PotionUtils.setPotion(new ItemStack(Items.POTION),
                    random.nextBoolean() ? Potions.HEALING : Potions.REGENERATION));
        }
        if (random.nextDouble() < clamp(0.04D + tierStrength(c.tier()) * 0.10D)) {
            add(inventory, PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    random.nextBoolean() ? Potions.SWIFTNESS : Potions.STRENGTH));
        }
    }
    private static int arrowCount(LoadoutRollContext c) { return 8 + (int) (tierStrength(c.tier()) * 18) + c.random().nextInt(5); }
    private static double rangedChance(LoadoutRollContext c) { return clamp(0.22D + tierStrength(c.tier()) * 0.28D + (isNether(c.spawnContext()) || isEnd(c.spawnContext()) ? 0.15D : 0.0D) + c.ageFactor() * 0.10D); }
    private static double shieldChance(LoadoutRollContext c) { return clamp((isEnd(c.spawnContext()) ? Config.loadoutShieldEndChance.get() : isNether(c.spawnContext()) ? Config.loadoutShieldNetherChance.get() : Config.loadoutShieldOverworldChance.get()) + tierStrength(c.tier()) * 0.18D + (c.spawnContext() == SpawnContext.BASTION || c.spawnContext() == SpawnContext.END_CITY ? 0.08D : 0.0D)); }
    private static double pickaxeChance(LoadoutRollContext c) { return clamp(0.68D + tierStrength(c.tier()) * 0.18D + (isNether(c.spawnContext()) || isEnd(c.spawnContext()) ? 0.10D : 0.0D)); }
    private static double axeChance(LoadoutRollContext c) { return clamp(0.62D + tierStrength(c.tier()) * 0.18D); }
    private static double utilityChance(LoadoutRollContext c) { return clamp(0.20D + tierStrength(c.tier()) * 0.25D + c.ageFactor() * 0.12D); }
    private static double pearlChance(LoadoutRollContext c) {
        double base = isEnd(c.spawnContext()) ? Config.loadoutPearlEndChance.get() : 0.04D;
        if (c.progression().endVisited()) base += Config.loadoutPostEndPearlBoost.get();
        return clamp(base + tierStrength(c.tier()) * 0.08D);
    }
    private static double waterBucketChance(LoadoutRollContext c) {
        if (c.dimension() == Level.NETHER) return 0.0D;
        return clamp(Config.loadoutWaterBucketChance.get() + tierStrength(c.tier()) * 0.08D
                + (isNether(c.spawnContext()) || isEnd(c.spawnContext()) ? 0.04D : 0.0D));
    }
    private static double goldenAppleChance(LoadoutRollContext c) { return clamp(0.12D + tierStrength(c.tier()) * 0.18D + c.ageFactor() * 0.10D + (isEnd(c.spawnContext()) ? 0.10D : 0.0D)); }
    private static double notchAppleChance(LoadoutRollContext c) { return clamp(0.01D + tierStrength(c.tier()) * 0.025D + c.ageFactor() * 0.015D + (c.spawnContext() == SpawnContext.END_CITY ? 0.025D : 0.0D)); }
    private static double totemChance(LoadoutRollContext c) {
        double base = isEnd(c.spawnContext()) ? Config.loadoutTotemEndChance.get() : 0.004D;
        if (c.progression().endVisited()) base += Config.loadoutPostEndTotemBoost.get();
        return clamp(base + tierStrength(c.tier()) * 0.018D + c.ageFactor() * 0.015D);
    }

    private static void add(List<ItemStack> inventory, ItemStack stack) { if (!stack.isEmpty()) inventory.add(stack); }
    private static void addTaggedFallback(List<ItemStack> inventory, Item preferred, TagKey<Item> tag, int count, RandomSource random) {
        Item selected = preferred.getDefaultInstance().is(tag) ? preferred : randomTaggedItem(tag, random);
        if (selected == null) return;
        int safeCount = Math.min(count, selected.getDefaultInstance().getMaxStackSize());
        add(inventory, new ItemStack(selected, Math.max(1, safeCount)));
    }
    private static Item randomTaggedItem(TagKey<Item> tag, RandomSource random) {
        List<Item> candidates = BuiltInRegistries.ITEM.getTag(tag)
                .map(values -> values.stream().map(holder -> holder.value()).toList()).orElse(List.of());
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }
    private static ItemStack stack(Item item) { return item.getDefaultInstance(); }
    private static ItemStack damage(ItemStack stack, RandomSource random, LoadoutRollContext context) {
        if (!stack.isDamageableItem()) return stack;
        int max = Math.max(1, stack.getMaxDamage() - 1);
        double wear = 0.08D + random.nextDouble() * (0.55D - context.ageFactor() * 0.15D);
        stack.setDamageValue(Math.min(max, (int) (max * wear)));
        return stack;
    }
    private static double tierStrength(CombatSkillTier tier) { return (4.0D - tier.ordinal()) / 4.0D; }
    private static boolean isNether(SpawnContext c) { return c == SpawnContext.NETHER_WILDS || c == SpawnContext.NETHER_FORTRESS || c == SpawnContext.BASTION; }
    private static boolean isEnd(SpawnContext c) { return c == SpawnContext.END_WILDS || c == SpawnContext.END_CITY; }
    private static double clamp(double value) { return Math.max(0.0D, Math.min(1.0D, value)); }

}
