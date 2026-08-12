package com.craftix.hostile_humans.entity.ai.squad;

public enum SquadAlertReason {
    SHARED_AGGRO(1),
    DIRECT_ATTACKER(2),
    PROTECT_RETREATING_ALLY(3);

    private final int priority;

    SquadAlertReason(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
