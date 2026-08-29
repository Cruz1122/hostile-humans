package com.craftix.hostile_humans.entity.ai.survival;

/** Stable diagnostic view; callers cannot mutate controller state. */
public record SurvivalSnapshot(
        SurvivalState state,
        SurvivalIntent intent,
        SurvivalFailureReason failureReason,
        long retryAtTick) {
}
