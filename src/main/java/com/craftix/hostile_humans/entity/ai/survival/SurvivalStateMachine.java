package com.craftix.hostile_humans.entity.ai.survival;

import java.util.Objects;

/**
 * Pure lifecycle state machine for survival actions.
 *
 * World-facing code is deliberately kept outside this class. This makes
 * interruptions, retries and completion deterministic and testable without a
 * Minecraft server.
 */
public final class SurvivalStateMachine {
    private SurvivalState state = SurvivalState.DORMANT;
    private SurvivalIntent intent;
    private SurvivalFailureReason failureReason;
    private long retryAtTick;

    public SurvivalSnapshot snapshot() {
        return new SurvivalSnapshot(state, intent, failureReason, retryAtTick);
    }

    public boolean requestAssessment(long now) {
        // PLANNING is retained after a successful action so a running goal can
        // choose its next task on the following tick. It must also be a valid
        // starting point when the goal selector declines an immediate action.
        if (state != SurvivalState.DORMANT && state != SurvivalState.BACKOFF
                && state != SurvivalState.PLANNING) return false;
        if (state == SurvivalState.BACKOFF && now < retryAtTick) return false;
        transition(SurvivalState.ASSESSING);
        failureReason = null;
        return true;
    }

    public void beginPlanning() {
        require(SurvivalState.ASSESSING);
        transition(SurvivalState.PLANNING);
    }

    public void plan(SurvivalIntent next) {
        require(SurvivalState.PLANNING);
        intent = next;
        if (next == null) {
            transition(SurvivalState.DORMANT);
        } else {
            transition(SurvivalState.ACQUIRE);
        }
    }

    public void acquired() {
        require(SurvivalState.ACQUIRE);
        transition(SurvivalState.NAVIGATE);
    }

    public void arrived() {
        require(SurvivalState.NAVIGATE);
        transition(SurvivalState.ACT);
    }

    public void collect() {
        require(SurvivalState.ACT);
        transition(SurvivalState.COLLECT);
    }

    public void waitForWorld() {
        require(SurvivalState.ACT);
        transition(SurvivalState.WAIT);
    }

    public void resumeActing() {
        require(SurvivalState.WAIT);
        transition(SurvivalState.ACT);
    }

    public void verify() {
        if (state != SurvivalState.ACT && state != SurvivalState.COLLECT && state != SurvivalState.WAIT) {
            throw new IllegalStateException("Cannot verify from " + state);
        }
        transition(SurvivalState.VERIFY);
    }

    public void succeeded() {
        require(SurvivalState.VERIFY);
        intent = null;
        failureReason = null;
        transition(SurvivalState.PLANNING);
    }

    public void fail(SurvivalFailureReason reason, long retryAtTick) {
        Objects.requireNonNull(reason, "reason");
        failureReason = reason;
        this.retryAtTick = retryAtTick;
        transition(SurvivalState.BACKOFF);
    }

    public void suspend() {
        if (state == SurvivalState.DORMANT || state == SurvivalState.BACKOFF) return;
        transition(SurvivalState.SUSPENDED);
        failureReason = SurvivalFailureReason.INTERRUPTED;
    }

    public void resumeAfterInterruption() {
        require(SurvivalState.SUSPENDED);
        transition(intent == null ? SurvivalState.DORMANT : SurvivalState.PLANNING);
        failureReason = null;
    }

    public void stop(SurvivalFailureReason reason) {
        intent = null;
        failureReason = reason;
        transition(SurvivalState.DORMANT);
    }

    public void reset() {
        state = SurvivalState.DORMANT;
        intent = null;
        failureReason = null;
        retryAtTick = 0L;
    }

    private void require(SurvivalState expected) {
        if (state != expected) throw new IllegalStateException("Expected " + expected + " but was " + state);
    }

    private void transition(SurvivalState next) {
        state = Objects.requireNonNull(next, "next");
    }
}
