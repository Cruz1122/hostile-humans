package com.craftix.hostile_humans.item;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HostileHumans.MOD_ID);
    public static final RegistryObject<Item> HUMAN_T1_SPAWN_EGG = tier("human_t1_spawn_egg", CombatSkillTier.T1, false);
    public static final RegistryObject<Item> HUMAN_T2_SPAWN_EGG = tier("human_t2_spawn_egg", CombatSkillTier.T2, false);
    public static final RegistryObject<Item> HUMAN_T3_SPAWN_EGG = tier("human_t3_spawn_egg", CombatSkillTier.T3, false);
    public static final RegistryObject<Item> HUMAN_T4_SPAWN_EGG = tier("human_t4_spawn_egg", CombatSkillTier.T4, false);
    public static final RegistryObject<Item> HUMAN_T5_SPAWN_EGG = tier("human_t5_spawn_egg", CombatSkillTier.T5, false);

    public static final RegistryObject<Item> HISPANIC_SPAWN_EGG = faction("hispanic_spawn_egg", PersonaFaction.HISPANIC_CREATORS, false);
    public static final RegistryObject<Item> INTERNATIONAL_SPAWN_EGG = faction("international_spawn_egg", PersonaFaction.INTERNATIONAL_CREATORS, false);
    public static final RegistryObject<Item> LEGENDS_SPAWN_EGG = faction("minecraft_legends_spawn_egg", PersonaFaction.MINECRAFT_LEGENDS, false);

    public static final RegistryObject<Item> ENCHANTED_HUMAN_T1_SPAWN_EGG = tier("enchanted_human_t1_spawn_egg", CombatSkillTier.T1, true);
    public static final RegistryObject<Item> ENCHANTED_HUMAN_T2_SPAWN_EGG = tier("enchanted_human_t2_spawn_egg", CombatSkillTier.T2, true);
    public static final RegistryObject<Item> ENCHANTED_HUMAN_T3_SPAWN_EGG = tier("enchanted_human_t3_spawn_egg", CombatSkillTier.T3, true);
    public static final RegistryObject<Item> ENCHANTED_HUMAN_T4_SPAWN_EGG = tier("enchanted_human_t4_spawn_egg", CombatSkillTier.T4, true);
    public static final RegistryObject<Item> ENCHANTED_HUMAN_T5_SPAWN_EGG = tier("enchanted_human_t5_spawn_egg", CombatSkillTier.T5, true);

    public static final RegistryObject<Item> ENCHANTED_HISPANIC_SPAWN_EGG = faction("enchanted_hispanic_spawn_egg", PersonaFaction.HISPANIC_CREATORS, true);
    public static final RegistryObject<Item> ENCHANTED_INTERNATIONAL_SPAWN_EGG = faction("enchanted_international_spawn_egg", PersonaFaction.INTERNATIONAL_CREATORS, true);
    public static final RegistryObject<Item> ENCHANTED_LEGENDS_SPAWN_EGG = faction("enchanted_minecraft_legends_spawn_egg", PersonaFaction.MINECRAFT_LEGENDS, true);

    private static RegistryObject<Item> tier(String id, CombatSkillTier tier, boolean enchanted) {
        return ITEMS.register(id, () -> new ConfiguredHumanSpawnEggItem(HumanSpawnEggSpec.tier(tier, enchanted),
                enchanted ? 0x8A2BE2 : 0xD97832, tierColor(tier, enchanted), properties(enchanted)));
    }

    private static RegistryObject<Item> faction(String id, PersonaFaction faction, boolean enchanted) {
        return ITEMS.register(id, () -> new ConfiguredHumanSpawnEggItem(HumanSpawnEggSpec.faction(faction, enchanted),
                factionColor(faction), enchanted ? 0xF5D742 : 0x352A2A, properties(enchanted)));
    }

    private static Item.Properties properties(boolean enchanted) {
        return new Item.Properties().rarity(enchanted ? Rarity.EPIC : Rarity.RARE);
    }

    private static int tierColor(CombatSkillTier tier, boolean enchanted) {
        int[] colors = {0xFF3030, 0xFF8C00, 0xFFD700, 0x32CD32, 0x6495ED};
        return enchanted ? 0xF5D742 : colors[tier.ordinal()];
    }

    private static int factionColor(PersonaFaction faction) {
        return switch (faction) {
            case HISPANIC_CREATORS -> 0xC0392B;
            case INTERNATIONAL_CREATORS -> 0x2980B9;
            case MINECRAFT_LEGENDS -> 0x8E44AD;
        };
    }
}
