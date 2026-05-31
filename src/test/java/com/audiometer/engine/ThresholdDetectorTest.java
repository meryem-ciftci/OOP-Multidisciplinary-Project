package com.audiometer.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audiometer.domain.Ear;
import com.audiometer.domain.ResponseRecord;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestPhase;
import com.audiometer.domain.ThresholdResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ThresholdDetectorTest {

    private final ThresholdDetector detector = new ThresholdDetector();

    @Test
    void detectsThresholdAtLowestAscendingLevel() {
        List<ResponseRecord> history = List.of(
                new ResponseRecord(1000, 30, ResponseType.HEARD, TestPhase.ASCENDING, Instant.EPOCH, Ear.LEFT),
                new ResponseRecord(1000, 30, ResponseType.NOT_HEARD, TestPhase.ASCENDING, Instant.EPOCH.plusSeconds(1), Ear.LEFT),
                new ResponseRecord(1000, 30, ResponseType.HEARD, TestPhase.ASCENDING, Instant.EPOCH.plusSeconds(2), Ear.LEFT));

        Optional<ThresholdResult> threshold = detector.detectThreshold(history, Ear.LEFT, 1000);

        assertTrue(threshold.isPresent());
        assertEquals(new ThresholdResult(1000, Ear.LEFT, 30), threshold.orElseThrow());
    }

    @Test
    void ignoresDescendingResponsesWhenDetectingThreshold() {
        List<ResponseRecord> history = List.of(
                new ResponseRecord(1000, 30, ResponseType.HEARD, TestPhase.DESCENDING, Instant.EPOCH, Ear.LEFT),
                new ResponseRecord(1000, 30, ResponseType.HEARD, TestPhase.DESCENDING, Instant.EPOCH.plusSeconds(1), Ear.LEFT),
                new ResponseRecord(1000, 30, ResponseType.HEARD, TestPhase.DESCENDING, Instant.EPOCH.plusSeconds(2), Ear.LEFT));

        assertTrue(detector.detectThreshold(history, Ear.LEFT, 1000).isEmpty());
    }
}
