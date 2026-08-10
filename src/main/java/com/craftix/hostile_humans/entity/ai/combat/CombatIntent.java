package com.craftix.hostile_humans.entity.ai.combat;

public record CombatIntent(
        CombatTactic tactic,
        CombatAction action,
        ShieldState shieldState,
        boolean allowMeleeAttack,
        boolean incomingProjectile,
        boolean visibleShieldDisabler,
        boolean targetBlocking,
        int commitmentUntilTick) {
    public static CombatIntent idle(ShieldState state) {
        return new CombatIntent(CombatTactic.APPROACH, CombatAction.HOLD_POSITION, state,
                true, false, false, false, 0);
    }
}
