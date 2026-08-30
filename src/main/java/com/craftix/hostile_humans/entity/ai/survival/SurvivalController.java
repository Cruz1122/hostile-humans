package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;

/**
 * Per-human façade around the pure state machine. The Minecraft goal only
 * translates world observations into lifecycle calls through this façade.
 */
public final class SurvivalController {
    private final Human human;
    private final SurvivalStateMachine machine = new SurvivalStateMachine();

    public SurvivalController(Human human) {
        this.human = human;
    }

    public boolean requestAssessment() {
        return machine.requestAssessment(human.level().getGameTime());
    }

    public void beginPlanning() {
        machine.beginPlanning();
    }

    public void plan(SurvivalIntent intent) {
        machine.plan(intent);
    }

    public void acquired() {
        machine.acquired();
    }

    public void arrived() {
        machine.arrived();
    }

    /** Repairs a delayed Forge lifecycle callback without exposing transitions to goals. */
    public void ensureActing() {
        switch (machine.snapshot().state()) {
            case ACQUIRE -> machine.acquired();
            case NAVIGATE -> machine.arrived();
            case WAIT -> machine.resumeActing();
            default -> { }
        }
    }

    /** Completes the current world task idempotently after its postcondition is observed. */
    public void completeTask() {
        ensureActing();
        if (machine.snapshot().state() == SurvivalState.ACT) machine.collect();
        if (machine.snapshot().state() == SurvivalState.COLLECT) machine.verify();
        if (machine.snapshot().state() == SurvivalState.VERIFY) machine.succeeded();
    }

    public void collect() {
        machine.collect();
    }

    public void waitForWorld() {
        machine.waitForWorld();
    }

    public void verify() {
        machine.verify();
    }

    public void succeeded() {
        machine.succeeded();
    }

    public void fail(SurvivalFailureReason reason, int retryTicks) {
        machine.fail(reason, human.level().getGameTime() + Math.max(0, retryTicks));
    }

    public void suspend() {
        machine.suspend();
    }

    public boolean isSuspended() {
        return machine.snapshot().state() == SurvivalState.SUSPENDED;
    }

    /** Restores an interrupted intent to the normal acquire/act lifecycle. */
    public void resumeAfterInterruption() {
        if (!isSuspended()) return;
        machine.resumeAfterInterruption();
        SurvivalIntent intent = machine.snapshot().intent();
        if (intent != null) machine.plan(intent);
    }

    /** Drops a completed or expired goal lifecycle without reporting a false interruption. */
    public void reset() {
        machine.reset();
    }

    public void stop(SurvivalFailureReason reason) {
        machine.stop(reason);
    }

    /** Completes a planner action that has no world tick of its own. */
    public void completeImmediate(SurvivalIntent intent) {
        if (machine.snapshot().state() == SurvivalState.ASSESSING) machine.beginPlanning();
        machine.plan(intent);
        machine.acquired();
        machine.arrived();
        machine.verify();
        machine.succeeded();
    }

    public SurvivalSnapshot snapshot() {
        return machine.snapshot();
    }
}
