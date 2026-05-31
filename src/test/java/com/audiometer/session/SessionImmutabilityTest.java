package com.audiometer.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.audiometer.domain.ResponseRecord;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestSession;
import com.audiometer.domain.TestState;
import com.audiometer.domain.ThresholdResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Robustness tests for session immutability and edge cases.
 *
 * <p>These tests verify that TestSession maintains immutability guarantees and that external
 * modifications cannot affect internal state.
 */
class SessionImmutabilityTest {

    private final SessionManager manager = new SessionManager();

    @Test
    void sessionHistoryIsImmutable() {
        TestSession session = manager.initialize();
        
        // Create a mutable list
        List<ResponseRecord> mutableHistory = new ArrayList<>();
        mutableHistory.add(new ResponseRecord(1000, 40, ResponseType.HEARD, 
            com.audiometer.domain.TestPhase.ASCENDING, Instant.now(), 
            com.audiometer.domain.Ear.LEFT));
        
        TestSession updated = session.withResponseHistory(mutableHistory);
        
        // Try to mutate the original list
        mutableHistory.add(new ResponseRecord(2000, 50, ResponseType.NOT_HEARD,
            com.audiometer.domain.TestPhase.ASCENDING, Instant.now(),
            com.audiometer.domain.Ear.LEFT));
        
        // Session should still have only 1 record
        assertEquals(1, updated.responseHistory().size());
    }

    @Test
    void sessionThresholdsAreImmutable() {
        TestSession session = manager.initialize();
        
        // Create a mutable list
        List<ThresholdResult> mutableThresholds = new ArrayList<>();
        ThresholdResult result = new ThresholdResult(1000, com.audiometer.domain.Ear.LEFT, 20);
        mutableThresholds.add(result);
        
        TestSession updated = session.withThresholds(mutableThresholds);
        
        // Try to mutate the original list
        mutableThresholds.add(new ThresholdResult(2000, com.audiometer.domain.Ear.LEFT, 25));
        
        // Session should still have only 1 threshold
        assertEquals(1, updated.thresholds().size());
    }

    @Test
    void withMethodsReturnNewInstances() {
        TestSession session = manager.initialize();
        
        TestSession updated1 = session.withCurrentDbLevel(50);
        TestSession updated2 = session.withCurrentDbLevel(60);
        
        // All should be different instances
        assertNotSame(session, updated1);
        assertNotSame(session, updated2);
        assertNotSame(updated1, updated2);
        
        // Original unchanged
        assertEquals(40, session.currentDbLevel());
        assertEquals(50, updated1.currentDbLevel());
        assertEquals(60, updated2.currentDbLevel());
    }

    @Test
    void chainedModificationsCreateUniqueInstances() {
        TestSession session = manager.initialize();
        
        TestSession modified = session
            .withCurrentDbLevel(50)
            .withState(TestState.PLAYING_TONE)
            .withCurrentFrequency(2000);
        
        // Original unchanged
        assertEquals(40, session.currentDbLevel());
        assertEquals(TestState.INITIALIZING, session.state());
        assertEquals(1000, session.currentFrequency());
        
        // New instance has changes
        assertEquals(50, modified.currentDbLevel());
        assertEquals(TestState.PLAYING_TONE, modified.state());
        assertEquals(2000, modified.currentFrequency());
    }

    @Test
    void repeatedTimeoutCyclesHandledCorrectly() {
        TestSession session = manager.initialize();
        session = manager.startTest(session);
        session = manager.playTone(session);
        session = manager.awaitResponse(session);
        
        // Record multiple timeouts
        for (int i = 0; i < 5; i++) {
            session = manager.handleTimeout(session, Instant.now());
            assertEquals(i + 1, session.responseHistory().size());
        }
        
        // All should be recorded as NOT_HEARD
        for (var record : session.responseHistory()) {
            assertEquals(ResponseType.TIMEOUT, record.responseType());
        }
    }

    @Test
    void frequencyBoundaryTests() {
        TestSession session = manager.initialize();
        
        // Valid frequencies: 250, 500, 1000, 2000, 4000, 8000
        TestSession atMin = session.withCurrentFrequency(250);
        TestSession atMax = session.withCurrentFrequency(8000);
        TestSession inMiddle = session.withCurrentFrequency(1000);
        
        assertEquals(250, atMin.currentFrequency());
        assertEquals(8000, atMax.currentFrequency());
        assertEquals(1000, inMiddle.currentFrequency());
    }

    @Test
    void dbLevelBoundaryTests() {
        TestSession session = manager.initialize();
        
        // Valid dB range: 0-120
        TestSession atMin = session.withCurrentDbLevel(0);
        TestSession atMax = session.withCurrentDbLevel(120);
        TestSession inMiddle = session.withCurrentDbLevel(60);
        
        assertEquals(0, atMin.currentDbLevel());
        assertEquals(120, atMax.currentDbLevel());
        assertEquals(60, inMiddle.currentDbLevel());
    }
}
