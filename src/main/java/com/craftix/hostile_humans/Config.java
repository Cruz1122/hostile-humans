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
        SERVER_BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
        SERVER_SPEC = SERVER_BUILDER.build();
    }
}
