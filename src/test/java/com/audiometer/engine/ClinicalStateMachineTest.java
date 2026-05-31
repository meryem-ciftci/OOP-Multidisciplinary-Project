package com.audiometer.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.audiometer.domain.TestState;
import org.junit.jupiter.api.Test;

class ClinicalStateMachineTest {

    private final ClinicalStateMachine stateMachine = new ClinicalStateMachine();

    @Test
    void transitionsToWaitingResponseAfterPlayingTone() {
        assertEquals(TestState.WAITING_RESPONSE, stateMachine.transition(TestState.PLAYING_TONE, ClinicalEvent.WAIT_FOR_RESPONSE));
    }

    @Test
    void rejectsIllegalStateTransition() {
        assertThrows(IllegalStateException.class,
                () -> stateMachine.transition(TestState.IDLE, ClinicalEvent.PROCESS_RESPONSE));
    }
}
