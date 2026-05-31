package com.audiometer.testsupport;

import com.audiometer.domain.Ear;
import com.audiometer.domain.ResponseRecord;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestPhase;
import java.time.Instant;
import java.util.List;

public final class ClinicalScenarioBuilder {

    private ClinicalScenarioBuilder() {
    }

    public static List<ResponseRecord> thresholdAtLevel(int frequency, Ear ear, int level) {
        return List.of(
                new ResponseRecord(frequency, level, ResponseType.HEARD, TestPhase.ASCENDING, Instant.EPOCH, ear),
                new ResponseRecord(frequency, level, ResponseType.NOT_HEARD, TestPhase.ASCENDING, Instant.EPOCH.plusSeconds(1), ear),
                new ResponseRecord(frequency, level, ResponseType.HEARD, TestPhase.ASCENDING, Instant.EPOCH.plusSeconds(2), ear));
    }
}
