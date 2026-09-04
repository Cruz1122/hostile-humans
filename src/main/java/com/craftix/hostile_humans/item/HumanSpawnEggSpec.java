package com.craftix.hostile_humans.item;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.persona.PersonaFaction;

/** Immutable configuration carried by a configured human spawn egg. */
public record HumanSpawnEggSpec(CombatSkillTier tier, PersonaFaction faction, boolean enchanted) {
    public static HumanSpawnEggSpec tier(CombatSkillTier tier, boolean enchanted) {
        return new HumanSpawnEggSpec(tier, null, enchanted);
    }

    public static HumanSpawnEggSpec faction(PersonaFaction faction, boolean enchanted) {
        return new HumanSpawnEggSpec(null, faction, enchanted);
    }
}
