package com.audiometer.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.audiometer.domain.TestState;
import org.junit.jupiter.api.Test;

/**
 * Robustness tests for state machine transition validation.
 *
 * <p>These tests ensure that illegal state transitions are properly rejected and that the state
 * machine maintains invariants.
 */
class ClinicalStateMachineRobustnessTest {

    private final ClinicalStateMachine machine = new ClinicalStateMachine();

    @Test
    void rejectsInvalidTransitionFromIdle() {
        assertFalse(machine.isValidTransition(TestState.IDLE, ClinicalEvent.PLAY_TONE));
        assertFalse(machine.isValidTransition(TestState.IDLE, ClinicalEvent.PROCESS_RESPONSE));
        assertFalse(machine.isValidTransition(TestState.IDLE, ClinicalEvent.THRESHOLD_FOUND));
    }

    @Test
    void rejectsInvalidTransitionFromPlayingTone() {
        assertFalse(machine.isValidTransition(TestState.PLAYING_TONE, ClinicalEvent.START_SESSION));
        assertFalse(machine.isValidTransition(TestState.PLAYING_TONE, ClinicalEvent.PROCESS_RESPONSE));
        assertFalse(machine.isValidTransition(TestState.PLAYING_TONE, ClinicalEvent.THRESHOLD_FOUND));
    }

    @Test
    void rejectsInvalidTransitionFromWaitingResponse() {
        assertFalse(machine.isValidTransition(TestState.WAITING_RESPONSE, ClinicalEvent.PLAY_TONE));
        assertFalse(machine.isValidTransition(TestState.WAITING_RESPONSE, ClinicalEvent.START_SESSION));
    }

    @Test
    void throwsOnIllegalTransition() {
        assertThrows(IllegalStateException.class, () -> 
            machine.transition(TestState.IDLE, ClinicalEvent.PLAY_TONE));
        
        assertThrows(IllegalStateException.class, () -> 
            machine.transition(TestState.PLAYING_TONE, ClinicalEvent.PROCESS_RESPONSE));
        
        assertThrows(IllegalStateException.class, () -> 
            machine.transition(TestState.THRESHOLD_FOUND, ClinicalEvent.PLAY_TONE));
    }

    @Test
    void allowsOnlyValidTransitionsFromThresholdFound() {
        assert machine.isValidTransition(TestState.THRESHOLD_FOUND, ClinicalEvent.ADVANCE_FREQUENCY);
        assert machine.isValidTransition(TestState.THRESHOLD_FOUND, ClinicalEvent.ADVANCE_EAR);
        
        assertFalse(machine.isValidTransition(TestState.THRESHOLD_FOUND, ClinicalEvent.START_SESSION));
        assertFalse(machine.isValidTransition(TestState.THRESHOLD_FOUND, ClinicalEvent.PLAY_TONE));
        assertFalse(machine.isValidTransition(TestState.THRESHOLD_FOUND, ClinicalEvent.WAIT_FOR_RESPONSE));
    }

    @Test
    void allowsErrorTransitionsFromMultipleStates() {
        assert machine.isValidTransition(TestState.INITIALIZING, ClinicalEvent.ERROR);
        assert machine.isValidTransition(TestState.WAITING_RESPONSE, ClinicalEvent.ERROR);
        
        // But not from all states
        assertFalse(machine.isValidTransition(TestState.IDLE, ClinicalEvent.ERROR));
        assertFalse(machine.isValidTransition(TestState.PLAYING_TONE, ClinicalEvent.ERROR));
    }

    @Test
    void resetsFromErrorAndTestFinished() {
        var errorNext = machine.transition(TestState.ERROR, ClinicalEvent.RESET);
        assert errorNext == TestState.IDLE;
        
        var finishedNext = machine.transition(TestState.TEST_FINISHED, ClinicalEvent.RESET);
        assert finishedNext == TestState.IDLE;
    }
}
