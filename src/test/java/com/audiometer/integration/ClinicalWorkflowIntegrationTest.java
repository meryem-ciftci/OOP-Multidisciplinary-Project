package com.audiometer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audiometer.domain.Ear;
import com.audiometer.domain.TestPhase;
import com.audiometer.domain.TestSession;
import com.audiometer.domain.TestState;
import com.audiometer.session.SessionManager;
import com.audiometer.testsupport.ClinicalScenarioBuilder;
import com.audiometer.util.ClinicalConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicalWorkflowIntegrationTest {

    private final SessionManager manager = new SessionManager();

    @Test
    void simulatesThresholdDetectionAndEarProgression() {
        TestSession session = manager.initialize();

        for (int index = 0; index < ClinicalConstants.ALLOWED_FREQUENCIES.size(); index++) {
            int frequency = ClinicalConstants.ALLOWED_FREQUENCIES.get(index);
            session = session.withCurrentEar(Ear.LEFT)
                    .withCurrentFrequency(frequency)
                    .withCurrentDbLevel(ClinicalConstants.INITIAL_DB)
                    .withCurrentPhase(TestPhase.ASCENDING)
                    .withFrequencyIndex(index)
                    .withResponseHistory(ClinicalScenarioBuilder.thresholdAtLevel(frequency, Ear.LEFT, 30))
                    .withState(TestState.PROCESSING_RESPONSE)
                    .withThresholds(List.of());

            session = manager.resolveAfterProcessing(session);
            assertEquals(TestState.THRESHOLD_FOUND, session.state());

            session = manager.advanceAfterThreshold(session);
            if (index < ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1) {
                assertEquals(TestState.NEXT_FREQUENCY, session.state());
            } else {
                assertEquals(TestState.NEXT_EAR, session.state());
                assertEquals(Ear.RIGHT, session.currentEar());
            }
        }

        session = session.withCurrentEar(Ear.RIGHT)
                .withCurrentFrequency(ClinicalConstants.ALLOWED_FREQUENCIES.get(ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1))
                .withCurrentDbLevel(ClinicalConstants.INITIAL_DB)
                .withCurrentPhase(TestPhase.ASCENDING)
                .withFrequencyIndex(ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1)
                .withResponseHistory(ClinicalScenarioBuilder.thresholdAtLevel(8000, Ear.RIGHT, 30))
                .withThresholds(List.of())
                .withState(TestState.PROCESSING_RESPONSE);

        session = manager.resolveAfterProcessing(session);
        assertEquals(TestState.THRESHOLD_FOUND, session.state());

        session = manager.advanceAfterThreshold(session);
        assertEquals(TestState.TEST_FINISHED, session.state());
        assertTrue(session.sessionCompleted());
    }
}
