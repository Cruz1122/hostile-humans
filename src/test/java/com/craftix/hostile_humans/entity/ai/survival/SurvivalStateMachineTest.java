package com.craftix.hostile_humans.entity.ai.survival;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalStateMachineTest {
    @Test
    void completesGatherAndReturnsToPlanningWithoutNullState() {
        SurvivalStateMachine machine = new SurvivalStateMachine();
        SurvivalIntent intent = new SurvivalIntent(SurvivalTask.GATHER, SurvivalObjective.WOOD_BOOTSTRAP);

        assertTrue(machine.requestAssessment(0));
        machine.beginPlanning();
        machine.plan(intent);
        machine.acquired();
        machine.arrived();
        machine.collect();
        machine.verify();
        machine.succeeded();

        assertEquals(SurvivalState.PLANNING, machine.snapshot().state());
        assertNull(machine.snapshot().intent());
        assertTrue(machine.requestAssessment(1));
        assertEquals(SurvivalState.ASSESSING, machine.snapshot().state());
    }

    @Test
    void interruptionSuspendsAndResumesExistingIntent() {
        SurvivalStateMachine machine = new SurvivalStateMachine();
        SurvivalIntent intent = new SurvivalIntent(SurvivalTask.SMELT, SurvivalObjective.IRON_GEAR);

        machine.requestAssessment(0);
        machine.beginPlanning();
        machine.plan(intent);
        machine.acquired();
        machine.arrived();
        machine.suspend();
        assertEquals(SurvivalState.SUSPENDED, machine.snapshot().state());
        machine.resumeAfterInterruption();

        assertEquals(SurvivalState.PLANNING, machine.snapshot().state());
        assertEquals(intent, machine.snapshot().intent());
    }

    @Test
    void backoffDoesNotBusyLoopBeforeDeadline() {
        SurvivalStateMachine machine = new SurvivalStateMachine();
        machine.requestAssessment(0);
        machine.beginPlanning();
        machine.plan(new SurvivalIntent(SurvivalTask.GATHER, SurvivalObjective.STONE_TOOLS));
        machine.fail(SurvivalFailureReason.NO_PATH, 50);

        assertFalse(machine.requestAssessment(49));
        assertTrue(machine.requestAssessment(50));
        assertEquals(SurvivalState.ASSESSING, machine.snapshot().state());
    }

    @Test
    void plannerMapsDiamondNeedToGearObjective() {
        assertEquals(SurvivalObjective.DIAMOND_GEAR,
                SurvivalPlanner.objectiveFor(SquadNeed.DIAMOND));
    }
}
