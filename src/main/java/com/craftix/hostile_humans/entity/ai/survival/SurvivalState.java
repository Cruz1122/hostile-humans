package com.craftix.hostile_humans.entity.ai.survival;

/** States owned by the survival controller. Null is never used as a state. */
public enum SurvivalState {
    DORMANT,
    ASSESSING,
    PLANNING,
    ACQUIRE,
    NAVIGATE,
    ACT,
    COLLECT,
    WAIT,
    VERIFY,
    SUSPENDED,
    BACKOFF
}
