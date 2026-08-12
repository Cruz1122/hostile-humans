package com.craftix.hostile_humans.persona;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;

public record PersonaDefinition(
        String id,
        String displayName,
        String minecraftUsername,
        String minecraftUsernameStatus,
        String uuid,
        String skinUrl,
        SkinStrategy skinStrategy,
        PersonaFaction faction,
        int tier,
        boolean uniqueWhileAlive,
        String legendKind
) {
    public CombatSkillTier combatSkillTier() {
        return CombatSkillTier.values()[tier - 1];
    }
}
