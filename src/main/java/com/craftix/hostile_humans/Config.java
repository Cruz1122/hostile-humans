package com.craftix.hostile_humans;

import com.craftix.hostile_humans.entity.entities.SpawnerEntity;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SERVER_SPEC;

    public static ForgeConfigSpec.ConfigValue<String> disabledStructures;
    public static ForgeConfigSpec.ConfigValue<Integer> maxTargeting;
    public static ForgeConfigSpec.ConfigValue<Double> greetChance;
    public static ForgeConfigSpec.ConfigValue<Double> runAwayMiddleFightChance;
    public static ForgeConfigSpec.ConfigValue<Double> fleeHpPercent;
    public static ForgeConfigSpec.ConfigValue<Double> healCombatPercent;
    public static ForgeConfigSpec.ConfigValue<Double> preAttackBuffChance;
    public static ForgeConfigSpec.ConfigValue<Double> midBattleBuffInsteadOfRunChance;
    public static ForgeConfigSpec.ConfigValue<Double> meleeFlurryChance;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeAttackCooldownMin;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeAttackCooldownMax;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeFlurryHitsMin;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeFlurryHitsMax;
    public static ForgeConfigSpec.ConfigValue<Integer> throwPotionsEvery;
    public static ForgeConfigSpec.ConfigValue<Integer> roamerNaturalSpawnRoll;
    public static ForgeConfigSpec.ConfigValue<Integer> battleEventNaturalSpawnRoll;
    public static ForgeConfigSpec.ConfigValue<Boolean> runJump;
    public static ForgeConfigSpec.ConfigValue<Boolean> attackJump;
    public static ForgeConfigSpec.ConfigValue<Double> underwaterWaterPotionChance;
    public static ForgeConfigSpec.ConfigValue<Integer> postBreathNoSwimTicks;
    public static ForgeConfigSpec.ConfigValue<Boolean> patreonNames;
    public static ForgeConfigSpec.ConfigValue<Boolean> noWaystones;
    public static ForgeConfigSpec.ConfigValue<Double> midBattleBuffInsteadOfRunTestChance;
    public static ForgeConfigSpec.EnumValue<SpawnerEntity.SpawnType> eventType;
    public static ForgeConfigSpec.BooleanValue enableFallbackToolWeapons;
    public static ForgeConfigSpec.BooleanValue enableCobwebPlacement;
    public static ForgeConfigSpec.IntValue cobwebCooldownTicks;
    public static ForgeConfigSpec.IntValue maxCobwebsPerCombat;
    public static ForgeConfigSpec.DoubleValue cobwebPlacementReach;
    public static ForgeConfigSpec.BooleanValue enableShieldTactics;
    public static ForgeConfigSpec.BooleanValue enableProjectileBlocking;
    public static ForgeConfigSpec.BooleanValue enableShieldBreaking;
    public static ForgeConfigSpec.BooleanValue enableEnderPearlTactics;
    public static ForgeConfigSpec.IntValue enderPearlCooldownTicks;
    public static ForgeConfigSpec.DoubleValue enderPearlOffensiveDistance;
    public static ForgeConfigSpec.DoubleValue enderPearlMaximumDistance;
    public static ForgeConfigSpec.BooleanValue enableWaterBucketTactics;
    public static ForgeConfigSpec.IntValue waterRecoveryDelayTicks;
    public static ForgeConfigSpec.BooleanValue enablePillaring;
    public static ForgeConfigSpec.BooleanValue enableBridging;
    public static ForgeConfigSpec.BooleanValue enableNavigationMining;
    public static ForgeConfigSpec.IntValue maxPillarHeight;
    public static ForgeConfigSpec.IntValue maxPillarBlocksPerPursuit;
    public static ForgeConfigSpec.IntValue maxBridgeLength;
    public static ForgeConfigSpec.IntValue maxBlocksPlacedPerPursuit;
    public static ForgeConfigSpec.IntValue maxBlocksBrokenPerPursuit;
    public static ForgeConfigSpec.IntValue maxMiningBlocksPerRecovery;
    public static ForgeConfigSpec.IntValue pillarPlacementCooldownTicks;
    public static ForgeConfigSpec.IntValue bridgePlacementCooldownTicks;
    public static ForgeConfigSpec.BooleanValue allowBridgeOverLava;
    public static ForgeConfigSpec.BooleanValue allowMiningWithoutCorrectTool;
    public static ForgeConfigSpec.DoubleValue miningSpeedMultiplier;
    public static ForgeConfigSpec.BooleanValue enableChestLooting;
    public static ForgeConfigSpec.IntValue chestSearchRadius;
    public static ForgeConfigSpec.IntValue chestSearchIntervalTicks;
    public static ForgeConfigSpec.IntValue chestRevisitCooldownTicks;
    public static ForgeConfigSpec.BooleanValue enableSurvivalProgression;
    public static ForgeConfigSpec.IntValue resourceScanRadius;
    public static ForgeConfigSpec.BooleanValue allowHiddenOreMining;
    public static ForgeConfigSpec.IntValue needsEvaluationIntervalTicks;
    public static ForgeConfigSpec.IntValue explorationRadius;
    public static ForgeConfigSpec.IntValue survivalPathBudgetPerTick;
    public static ForgeConfigSpec.IntValue survivalScanBudgetPerTick;
    public static ForgeConfigSpec.BooleanValue enableNaturalHumanSpawning;
    public static ForgeConfigSpec.IntValue spawnAttemptIntervalTicks;
    public static ForgeConfigSpec.IntValue minSpawnDistance;
    public static ForgeConfigSpec.IntValue maxSpawnDistance;
    public static ForgeConfigSpec.IntValue nearbyHumanCapPerPlayer;
    public static ForgeConfigSpec.IntValue dimensionHumanCap;
    public static ForgeConfigSpec.DoubleValue legendSpawnWeight;
    public static ForgeConfigSpec.DoubleValue spawnSurfaceChance;
    public static ForgeConfigSpec.DoubleValue spawnCaveChance;
    public static ForgeConfigSpec.DoubleValue spawnStructureChance;
    public static ForgeConfigSpec.DoubleValue spawnNetherChance;
    public static ForgeConfigSpec.DoubleValue spawnFortressChance;
    public static ForgeConfigSpec.DoubleValue spawnEndChance;
    public static ForgeConfigSpec.DoubleValue spawnEndCityChance;
    public static ForgeConfigSpec.DoubleValue squadChance;
    public static ForgeConfigSpec.DoubleValue largeSquadChance;
    public static ForgeConfigSpec.IntValue loadoutAgeCapTicks;
    public static ForgeConfigSpec.DoubleValue loadoutOverworldNetheriteChance;
    public static ForgeConfigSpec.DoubleValue loadoutNetherNetheriteChance;
    public static ForgeConfigSpec.DoubleValue loadoutEndNetheriteChance;
    public static ForgeConfigSpec.DoubleValue loadoutPostEndOverworldNetheriteChance;
    public static ForgeConfigSpec.DoubleValue loadoutPostEndNetherNetheriteChance;
    public static ForgeConfigSpec.DoubleValue loadoutOverworldNetheriteCap;
    public static ForgeConfigSpec.DoubleValue loadoutNetherNetheriteCap;
    public static ForgeConfigSpec.DoubleValue loadoutEndNetheriteCap;
    public static ForgeConfigSpec.DoubleValue loadoutDiamondOverworldChance;
    public static ForgeConfigSpec.DoubleValue loadoutDiamondNetherChance;
    public static ForgeConfigSpec.DoubleValue loadoutDiamondEndChance;
    public static ForgeConfigSpec.DoubleValue loadoutPostEndDiamondBoost;
    public static ForgeConfigSpec.DoubleValue loadoutPearlEndChance;
    public static ForgeConfigSpec.DoubleValue loadoutPostEndPearlBoost;
    public static ForgeConfigSpec.DoubleValue loadoutWaterBucketChance;
    public static ForgeConfigSpec.DoubleValue loadoutTotemEndChance;
    public static ForgeConfigSpec.DoubleValue loadoutPostEndTotemBoost;
    public static ForgeConfigSpec.DoubleValue loadoutShieldOverworldChance;
    public static ForgeConfigSpec.DoubleValue loadoutShieldNetherChance;
    public static ForgeConfigSpec.DoubleValue loadoutShieldEndChance;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantChanceT1;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantChanceT2;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantChanceT3;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantChanceT4;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantChanceT5;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantLevel2Chance;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantLevel3Chance;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantLevel4Chance;
    public static ForgeConfigSpec.DoubleValue loadoutEnchantLevel5Chance;
    public static ForgeConfigSpec.DoubleValue deathXpScale;

    static {
        BUILDER.push("Hostile Humans Settings");
        disabledStructures = BUILDER.comment("Disabled Structures (comma separated) ex. cottage, cozy_spruce_house, desert_house, desert_house_2, desert_house_3, desert_house_4, farmhouse, fortress_bottom, fortress_top, igloo, large_desert_house, large_spruce_home, oak_house, oak_house_2, oak_house_3, oak_house_4, oak_house_5, savanna_house_2, spruce_cottage, spruce_fort, spruce_house, thin_spruce, tiny_acacia, tiny_igloo, tiny_spruce_house, tower, warehouse").define("disabled_structures", "");
        maxTargeting = BUILDER.comment("The max amount of humans that can attack you at the same time").define("max_targeting", 3);
        greetChance = BUILDER.comment("The chance to send a chat message to the player upon targeting them").define("greet_chance", 0.05d);
        runAwayMiddleFightChance = BUILDER.comment("Chance [0..1] that a low-health human chooses to flee during a combat encounter").defineInRange("run_away_middle_fight_chance", 0.5d, 0.0d, 1.0d);
        fleeHpPercent = BUILDER.comment("The % of hp to start fleeing").define("flee_hp", 0.15d);
        healCombatPercent = BUILDER.comment("The % of hp to attempt healing during combat").define("heal_combat", 0.5d);
        preAttackBuffChance = BUILDER.comment("Chance that a human will use a pre-attack buff item once after spotting a player").define("pre_attack_buff_chance", 0.05d);
        midBattleBuffInsteadOfRunChance = BUILDER.comment("Tier 2 only: chance [0..1] that a human eats an enchanted golden apple instead of fleeing mid-fight at low health").defineInRange("mid_battle_buff_instead_of_run_chance", 0.10d, 0.0d, 1.0d);
        midBattleBuffInsteadOfRunTestChance = BUILDER.comment("Testing override for enchanted golden apple mid-fight chance. Set below 0 to use the normal chance.").defineInRange("mid_battle_buff_instead_of_run_test_chance", 0.0d, -1.0d, 1.0d);
        meleeAttackCooldownMin = BUILDER.comment("Minimum melee attack cooldown in ticks").define("melee_attack_cooldown_min", 7);
        meleeAttackCooldownMax = BUILDER.comment("Maximum melee attack cooldown in ticks").define("melee_attack_cooldown_max", 14);
        throwPotionsEvery = BUILDER.comment("Throw potions every x ticks").define("throw_potions_every", 20 * 100);
        roamerNaturalSpawnRoll = BUILDER.comment("Natural roamer spawn roll: 1 = always pass (very high spawn for testing), 200 = old default rarity").defineInRange("roamer_natural_spawn_roll", 200, 1, 10000);
        battleEventNaturalSpawnRoll = BUILDER.comment("Natural battle-event spawner roll: 1 = always pass (very high spawn for testing), 200 = old default rarity").defineInRange("battle_event_natural_spawn_roll", 200, 1, 10000);
        runJump = BUILDER.comment("Humans can run and jump (like a player)").define("run_jump", true);
        attackJump = BUILDER.comment("Humans can do fake melee attack jumps without applying critical damage").define("attack_jump", true);
        underwaterWaterPotionChance = BUILDER.comment("Chance [0..1], rolled at most once every 10 seconds, that a submerged tier 2 human drinks a water breathing potion").define("underwater_water_potion_chance", 0.04d);
        postBreathNoSwimTicks = BUILDER.comment("Ticks a human must remain at the surface before diving again after recovering air").define("post_breath_no_swim_ticks", 40);
        patreonNames = BUILDER.comment("Allow names of Patreon members to show up as viable names").define("patreon_names", true);
        noWaystones = BUILDER.comment("Should waystones not load in structures even with the mod present").define("no_waystones", false);
        eventType = BUILDER.comment("Which type of battle event should occur").defineEnum("battle_event", SpawnerEntity.SpawnType.Random);

        SERVER_BUILDER.push("tacticalEquipment");
        enableFallbackToolWeapons = SERVER_BUILDER.comment("Allow pickaxes, shovels and hoes as melee fallback weapons")
                .define("enableFallbackToolWeapons", true);
        enableCobwebPlacement = SERVER_BUILDER.comment("Allow humans to place tactical cobwebs while retreating")
                .define("enableCobwebPlacement", true);
        cobwebCooldownTicks = SERVER_BUILDER.comment("Cooldown between tactical cobweb placements")
                .defineInRange("cobwebCooldownTicks", 80, 20, 600);
        maxCobwebsPerCombat = SERVER_BUILDER.comment("Maximum tactical cobwebs placed by one human per combat")
                .defineInRange("maxCobwebsPerCombat", 2, 0, 8);
        cobwebPlacementReach = SERVER_BUILDER.comment("Maximum distance from the human for a tactical cobweb")
                .defineInRange("cobwebPlacementReach", 2.5D, 1.0D, 4.5D);
        enableShieldTactics = SERVER_BUILDER.comment("Enable observable tactical shield behavior")
                .define("enableShieldTactics", true);
        enableProjectileBlocking = SERVER_BUILDER.comment("Allow tactical shield reactions to visible incoming projectiles")
                .define("enableProjectileBlocking", true);
        enableShieldBreaking = SERVER_BUILDER.comment("Allow tactical switching to weapons that can disable visible shields")
                .define("enableShieldBreaking", true);
        enableEnderPearlTactics = SERVER_BUILDER.comment("Allow Humans to use ender pearls for offensive and defensive repositioning")
                .define("enableEnderPearlTactics", true);
        enderPearlCooldownTicks = SERVER_BUILDER.comment("Cooldown between tactical ender pearl throws")
                .defineInRange("enderPearlCooldownTicks", 100, 20, 600);
        enderPearlOffensiveDistance = SERVER_BUILDER.comment("Minimum target distance for an offensive ender pearl")
                .defineInRange("enderPearlOffensiveDistance", 10.0D, 4.0D, 32.0D);
        enderPearlMaximumDistance = SERVER_BUILDER.comment("Maximum target distance for an offensive ender pearl")
                .defineInRange("enderPearlMaximumDistance", 28.0D, 8.0D, 64.0D);
        enableWaterBucketTactics = SERVER_BUILDER.comment("Allow Humans to place and recover water with buckets")
                .define("enableWaterBucketTactics", true);
        waterRecoveryDelayTicks = SERVER_BUILDER.comment("Ticks before a tactical water source may be recovered")
                .defineInRange("waterRecoveryDelayTicks", 40, 10, 600);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("chestLooting");
        enableChestLooting = SERVER_BUILDER.comment("Allow humans to loot visible vanilla chests")
                .define("enableChestLooting", true);
        chestSearchRadius = SERVER_BUILDER.comment("Maximum chest search radius in blocks")
                .defineInRange("chestSearchRadius", 6, 1, 16);
        chestSearchIntervalTicks = SERVER_BUILDER.comment("Ticks between chest searches")
                .defineInRange("chestSearchIntervalTicks", 60, 20, 600);
        chestRevisitCooldownTicks = SERVER_BUILDER.comment("Ticks before revisiting the last looted chest")
                .defineInRange("chestRevisitCooldownTicks", 900, 100, 24000);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("worldNavigation");
        enablePillaring = SERVER_BUILDER.define("enablePillaring", true);
        enableBridging = SERVER_BUILDER.define("enableBridging", true);
        enableNavigationMining = SERVER_BUILDER.define("enableNavigationMining", true);
        maxPillarHeight = SERVER_BUILDER.defineInRange("maxPillarHeight", 4, 1, 8);
        maxPillarBlocksPerPursuit = SERVER_BUILDER.defineInRange("maxPillarBlocksPerPursuit", 6, 0, 16);
        maxBridgeLength = SERVER_BUILDER.defineInRange("maxBridgeLength", 3, 1, 3);
        maxBlocksPlacedPerPursuit = SERVER_BUILDER.defineInRange("maxBlocksPlacedPerPursuit", 10, 0, 32);
        maxBlocksBrokenPerPursuit = SERVER_BUILDER.defineInRange("maxBlocksBrokenPerPursuit", 8, 0, 32);
        maxMiningBlocksPerRecovery = SERVER_BUILDER.defineInRange("maxMiningBlocksPerRecovery", 3, 1, 3);
        pillarPlacementCooldownTicks = SERVER_BUILDER.defineInRange("pillarPlacementCooldownTicks", 8, 1, 40);
        bridgePlacementCooldownTicks = SERVER_BUILDER.defineInRange("bridgePlacementCooldownTicks", 8, 1, 40);
        allowBridgeOverLava = SERVER_BUILDER.define("allowBridgeOverLava", false);
        allowMiningWithoutCorrectTool = SERVER_BUILDER.define("allowMiningWithoutCorrectTool", false);
        miningSpeedMultiplier = SERVER_BUILDER.defineInRange("miningSpeedMultiplier", 4.0D, 0.1D, 5.0D);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("survivalProgression");
        enableSurvivalProgression = SERVER_BUILDER.comment("Allow needs-driven squad survival progression")
                .define("enableSurvivalProgression", true);
        resourceScanRadius = SERVER_BUILDER.comment("Maximum local radius for loaded resource searches")
                .defineInRange("resourceScanRadius", 32, 4, 64);
        allowHiddenOreMining = SERVER_BUILDER.comment("Allow survival progression to target loaded ores through solid blocks")
                .define("allowHiddenOreMining", true);
        needsEvaluationIntervalTicks = SERVER_BUILDER.comment("Ticks between squad-needs evaluations")
                .defineInRange("needsEvaluationIntervalTicks", 60, 40, 200);
        explorationRadius = SERVER_BUILDER.comment("Maximum radius for local progression exploration")
                .defineInRange("explorationRadius", 64, 12, 128);
        survivalPathBudgetPerTick = SERVER_BUILDER.comment("Maximum survival path requests per dimension and tick")
                .defineInRange("survivalPathBudgetPerTick", 100, 10, 1000);
        survivalScanBudgetPerTick = SERVER_BUILDER.comment("Maximum full survival resource scans per dimension and tick")
                .defineInRange("survivalScanBudgetPerTick", 50, 5, 500);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("naturalSpawning");
        enableNaturalHumanSpawning = SERVER_BUILDER.comment("Enable server-side natural Human encounters")
                .define("enableNaturalHumanSpawning", true);
        spawnAttemptIntervalTicks = SERVER_BUILDER.comment("Ticks between bounded natural spawn attempts")
                .defineInRange("spawnAttemptIntervalTicks", 120, 20, 12000);
        minSpawnDistance = SERVER_BUILDER.comment("Minimum horizontal distance from an eligible player")
                .defineInRange("minSpawnDistance", 24, 8, 128);
        maxSpawnDistance = SERVER_BUILDER.comment("Maximum distance from an eligible player")
                .defineInRange("maxSpawnDistance", 80, 24, 192);
        nearbyHumanCapPerPlayer = SERVER_BUILDER.comment("Maximum loaded Humans near one player")
                .defineInRange("nearbyHumanCapPerPlayer", 6, 1, 64);
        dimensionHumanCap = SERVER_BUILDER.comment("Maximum loaded Humans in one dimension")
                .defineInRange("dimensionHumanCap", 24, 1, 256);
        legendSpawnWeight = SERVER_BUILDER.comment("Fraction of natural encounters assigned to Minecraft Legends")
                .defineInRange("legendSpawnWeight", 0.02D, 0.0D, 1.0D);
        spawnSurfaceChance = SERVER_BUILDER.comment("Natural encounter chance for ordinary Overworld surface")
                .defineInRange("surfaceChance", 0.35D, 0.0D, 1.0D);
        spawnCaveChance = SERVER_BUILDER.comment("Natural encounter chance for caves and villages")
                .defineInRange("caveChance", 0.45D, 0.0D, 1.0D);
        spawnStructureChance = SERVER_BUILDER.comment("Natural encounter chance for tagged Overworld structures")
                .defineInRange("structureChance", 0.45D, 0.0D, 1.0D);
        spawnNetherChance = SERVER_BUILDER.comment("Natural encounter chance for Nether wilds")
                .defineInRange("netherChance", 0.35D, 0.0D, 1.0D);
        spawnFortressChance = SERVER_BUILDER.comment("Natural encounter chance for Nether fortresses and bastions")
                .defineInRange("fortressChance", 0.50D, 0.0D, 1.0D);
        spawnEndChance = SERVER_BUILDER.comment("Natural encounter chance for End wilds")
                .defineInRange("endChance", 0.35D, 0.0D, 1.0D);
        spawnEndCityChance = SERVER_BUILDER.comment("Natural encounter chance for End Cities")
                .defineInRange("endCityChance", 0.55D, 0.0D, 1.0D);
        squadChance = SERVER_BUILDER.comment("Chance that a natural encounter has more than one Human")
                .defineInRange("squadChance", 0.45D, 0.0D, 1.0D);
        largeSquadChance = SERVER_BUILDER.comment("Chance that a squad encounter has four or five Humans")
                .defineInRange("largeSquadChance", 0.22D, 0.0D, 1.0D);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("naturalLoadouts");
        loadoutAgeCapTicks = SERVER_BUILDER.comment("Game ticks at which natural loadout age scaling reaches its cap")
                .defineInRange("ageCapTicks", 5_184_000, 24_000, Integer.MAX_VALUE);
        loadoutOverworldNetheriteChance = SERVER_BUILDER.comment("Base Netherite quality chance in the Overworld after Netherite is unlocked")
                .defineInRange("overworldNetheriteChance", 0.04D, 0.0D, 1.0D);
        loadoutNetherNetheriteChance = SERVER_BUILDER.comment("Base Netherite quality chance in Nether wilds after Netherite is unlocked")
                .defineInRange("netherNetheriteChance", 0.12D, 0.0D, 1.0D);
        loadoutEndNetheriteChance = SERVER_BUILDER.comment("Base Netherite quality chance in the End after Netherite is unlocked")
                .defineInRange("endNetheriteChance", 0.30D, 0.0D, 1.0D);
        loadoutPostEndOverworldNetheriteChance = SERVER_BUILDER.comment("Effective Overworld Netherite chance after the End has been visited")
                .defineInRange("postEndOverworldNetheriteChance", 0.18D, 0.0D, 1.0D);
        loadoutPostEndNetherNetheriteChance = SERVER_BUILDER.comment("Effective Nether Netherite chance after the End has been visited")
                .defineInRange("postEndNetherNetheriteChance", 0.32D, 0.0D, 1.0D);
        loadoutOverworldNetheriteCap = SERVER_BUILDER.comment("Absolute Overworld cap for Netherite quality")
                .defineInRange("overworldNetheriteCap", 0.28D, 0.0D, 1.0D);
        loadoutNetherNetheriteCap = SERVER_BUILDER.comment("Absolute Nether cap for Netherite quality")
                .defineInRange("netherNetheriteCap", 0.45D, 0.0D, 1.0D);
        loadoutEndNetheriteCap = SERVER_BUILDER.comment("Absolute End cap for Netherite quality")
                .defineInRange("endNetheriteCap", 0.55D, 0.0D, 1.0D);
        loadoutDiamondOverworldChance = SERVER_BUILDER.comment("Base Diamond quality chance in the Overworld after Diamond is unlocked")
                .defineInRange("diamondOverworldChance", 0.16D, 0.0D, 1.0D);
        loadoutDiamondNetherChance = SERVER_BUILDER.comment("Base Diamond quality chance in Nether contexts")
                .defineInRange("diamondNetherChance", 0.58D, 0.0D, 1.0D);
        loadoutDiamondEndChance = SERVER_BUILDER.comment("Base Diamond quality chance in End contexts")
                .defineInRange("diamondEndChance", 0.82D, 0.0D, 1.0D);
        loadoutPostEndDiamondBoost = SERVER_BUILDER.comment("Additional Diamond quality bias after the End has been visited")
                .defineInRange("postEndDiamondBoost", 0.24D, 0.0D, 1.0D);
        loadoutPearlEndChance = SERVER_BUILDER.comment("Ender Pearl chance in End contexts")
                .defineInRange("pearlEndChance", 0.26D, 0.0D, 1.0D);
        loadoutPostEndPearlBoost = SERVER_BUILDER.comment("Additional Ender Pearl chance after the End has been visited")
                .defineInRange("postEndPearlBoost", 0.16D, 0.0D, 1.0D);
        loadoutWaterBucketChance = SERVER_BUILDER.comment("Base water bucket chance in natural Human loadouts")
                .defineInRange("waterBucketChance", 0.12D, 0.0D, 1.0D);
        loadoutTotemEndChance = SERVER_BUILDER.comment("Totem chance in End contexts")
                .defineInRange("totemEndChance", 0.018D, 0.0D, 1.0D);
        loadoutPostEndTotemBoost = SERVER_BUILDER.comment("Additional Totem chance after the End has been visited")
                .defineInRange("postEndTotemBoost", 0.012D, 0.0D, 1.0D);
        loadoutShieldOverworldChance = SERVER_BUILDER.comment("Base shield chance in the Overworld")
                .defineInRange("shieldOverworldChance", 0.68D, 0.0D, 1.0D);
        loadoutShieldNetherChance = SERVER_BUILDER.comment("Base shield chance in Nether contexts")
                .defineInRange("shieldNetherChance", 0.84D, 0.0D, 1.0D);
        loadoutShieldEndChance = SERVER_BUILDER.comment("Base shield chance in End contexts")
                .defineInRange("shieldEndChance", 0.88D, 0.0D, 1.0D);
        loadoutEnchantChanceT1 = SERVER_BUILDER.comment("Chance that each eligible T1 equipment item receives an enchantment")
                .defineInRange("enchantChanceT1", 0.82D, 0.0D, 1.0D);
        loadoutEnchantChanceT2 = SERVER_BUILDER.comment("Chance that each eligible T2 equipment item receives an enchantment")
                .defineInRange("enchantChanceT2", 0.74D, 0.0D, 1.0D);
        loadoutEnchantChanceT3 = SERVER_BUILDER.comment("Chance that each eligible T3 equipment item receives an enchantment")
                .defineInRange("enchantChanceT3", 0.64D, 0.0D, 1.0D);
        loadoutEnchantChanceT4 = SERVER_BUILDER.comment("Chance that each eligible T4 equipment item receives an enchantment")
                .defineInRange("enchantChanceT4", 0.54D, 0.0D, 1.0D);
        loadoutEnchantChanceT5 = SERVER_BUILDER.comment("Chance that each eligible T5 equipment item receives an enchantment")
                .defineInRange("enchantChanceT5", 0.44D, 0.0D, 1.0D);
        loadoutEnchantLevel2Chance = SERVER_BUILDER.comment("Relative chance of retaining enchantment level II or higher")
                .defineInRange("enchantLevel2Chance", 0.62D, 0.0D, 1.0D);
        loadoutEnchantLevel3Chance = SERVER_BUILDER.comment("Relative chance of retaining enchantment level III or higher")
                .defineInRange("enchantLevel3Chance", 0.34D, 0.0D, 1.0D);
        loadoutEnchantLevel4Chance = SERVER_BUILDER.comment("Relative chance of retaining enchantment level IV or higher")
                .defineInRange("enchantLevel4Chance", 0.16D, 0.0D, 1.0D);
        loadoutEnchantLevel5Chance = SERVER_BUILDER.comment("Relative chance of retaining enchantment level V")
                .defineInRange("enchantLevel5Chance", 0.06D, 0.0D, 1.0D);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("deathRewards");
        deathXpScale = SERVER_BUILDER.comment("Multiplier for the bounded Human death XP reward")
                .defineInRange("xpScale", 1.0D, 0.5D, 1.5D);
        SERVER_BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
        SERVER_SPEC = SERVER_BUILDER.build();
    }
}
