package com.audiometer.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandBuilderTest {

    private final CommandBuilder builder = new CommandBuilder();

    @Test
    void buildsCombinedCommandForFrequencyAndAmplitude() {
        assertEquals("FREQ:1000,AMP:40", builder.buildCombinedCommand(1000, 40).orElseThrow());
    }

    @Test
    void rejectsOutOfRangeFrequencyAndAmplitude() {
        // Frequency 200 is not in ALLOWED_FREQUENCIES
        assertTrue(builder.buildFrequencyCommand(200).isEmpty());
        // Amplitude 4096 exceeds MAX_AMPLITUDE (4095)
        assertTrue(builder.buildAmplitudeCommand(4096).isEmpty());
    }

    @Test
    void acceptsValidAmplitudeRangeValues() {
        // Valid amplitude at minimum
        assertEquals("AMP:0", builder.buildAmplitudeCommand(0).orElseThrow());
        // Valid amplitude at maximum
        assertEquals("AMP:4095", builder.buildAmplitudeCommand(4095).orElseThrow());
        // Valid amplitude in middle range
        assertEquals("AMP:2048", builder.buildAmplitudeCommand(2048).orElseThrow());
    }

    @Test
    void rejectNegativeAmplitude() {
        assertTrue(builder.buildAmplitudeCommand(-1).isEmpty());
    }
}
