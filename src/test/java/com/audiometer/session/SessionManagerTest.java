package com.audiometer.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audiometer.domain.Ear;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestSession;
import com.audiometer.domain.TestState;
import com.audiometer.util.ClinicalConstants;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionManagerTest {

    private final SessionManager manager = new SessionManager();

    @Test
    void initializesClinicalSessionForLeftEar() {
        TestSession session = manager.initialize();

        assertEquals(Ear.LEFT, session.currentEar());
        assertEquals(ClinicalConstants.START_FREQUENCY, session.currentFrequency());
        assertEquals(ClinicalConstants.INITIAL_DB, session.currentDbLevel());
        assertEquals(TestState.INITIALIZING, session.state());
        assertTrue(session.responseHistory().isEmpty());
    }

    @Test
    void handlesTimeoutAsClinicalNotHeard() {
        TestSession session = manager.initialize();
        session = manager.startTest(session);
        session = manager.playTone(session);
        session = manager.awaitResponse(session);

        TestSession processed = manager.handleTimeout(session, Instant.EPOCH.plusMillis(ClinicalConstants.TIMEOUT_MILLIS));

        assertEquals(TestState.PROCESSING_RESPONSE, processed.state());
        assertEquals(ResponseType.TIMEOUT, processed.responseHistory().get(0).responseType());
        assertEquals(45, processed.currentDbLevel());

        TestSession next = manager.resolveAfterProcessing(processed);
        assertEquals(TestState.PLAYING_TONE, next.state());
    }

    @Test
    void advancesToNextEarAfterFinalFrequency() {
        TestSession session = new TestSession(
                Ear.LEFT,
                ClinicalConstants.ALLOWED_FREQUENCIES.get(ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1),
                40,
                null,
                TestState.THRESHOLD_FOUND,
                List.of(),
                List.of(),
                ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1,
                false);

        TestSession advanced = manager.advanceAfterThreshold(session);

        assertEquals(TestState.NEXT_EAR, advanced.state());
        assertEquals(Ear.RIGHT, advanced.currentEar());
        assertEquals(ClinicalConstants.INITIAL_DB, advanced.currentDbLevel());
        assertEquals(0, advanced.frequencyIndex());
    }
}
