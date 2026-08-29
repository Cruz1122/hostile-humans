package com.craftix.hostile_humans.entity.ai.survival;

import java.util.Objects;

/** Immutable plan selected by the pure planner and executed by the adapter. */
public record SurvivalIntent(SurvivalTask task, SurvivalObjective objective) {
    public SurvivalIntent {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(objective, "objective");
    }
}
