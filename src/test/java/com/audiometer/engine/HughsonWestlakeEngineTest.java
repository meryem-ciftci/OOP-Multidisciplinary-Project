package com.audiometer.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.audiometer.domain.ResponseType;
import org.junit.jupiter.api.Test;

class HughsonWestlakeEngineTest {

    private final HughsonWestlakeEngine engine = new HughsonWestlakeEngine();

    @Test
    void decreasesDbWhenPatientHeardTone() {
        assertEquals(30, engine.nextDb(40, ResponseType.HEARD));
    }

    @Test
    void increasesDbWhenPatientDoesNotHearTone() {
        assertEquals(45, engine.nextDb(40, ResponseType.NOT_HEARD));
    }

    @Test
    void clampsDbAtSafeBounds() {
        assertEquals(0, engine.nextDb(0, ResponseType.HEARD));
        assertEquals(120, engine.nextDb(120, ResponseType.NOT_HEARD));
    }
}
