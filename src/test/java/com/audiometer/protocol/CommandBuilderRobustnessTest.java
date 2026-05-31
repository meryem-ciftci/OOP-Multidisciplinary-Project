package com.audiometer.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

/**
 * Robustness tests for protocol command builder with boundary and edge cases.
 *
 * <p>These tests ensure that command validation correctly enforces amplitude (0-4095) and
 * frequency boundaries.
 */
class CommandBuilderRobustnessTest {

    private final CommandBuilder builder = new CommandBuilder();

    // ==================== Amplitude Boundary Tests ====================

    @Test
    void acceptsMinimumAmplitude() {
        assertTrue(builder.buildAmplitudeCommand(0).isPresent());
        assertTrue(builder.isAmplitudeValid(0));
    }

    @Test
    void acceptsMaximumAmplitude() {
        assertTrue(builder.buildAmplitudeCommand(4095).isPresent());
        assertTrue(builder.isAmplitudeValid(4095));
    }

    @Test
    void rejectsAmplitudeJustBelowMinimum() {
        assertFalse(builder.buildAmplitudeCommand(-1).isPresent());
        assertFalse(builder.isAmplitudeValid(-1));
    }

    @Test
    void rejectsAmplitudeJustAboveMaximum() {
        assertFalse(builder.buildAmplitudeCommand(4096).isPresent());
        assertFalse(builder.isAmplitudeValid(4096));
    }

    @Test
    void rejectsNegativeAmplitudes() {
        assertFalse(builder.isAmplitudeValid(-1));
        assertFalse(builder.isAmplitudeValid(-100));
        assertFalse(builder.isAmplitudeValid(-4095));
        assertFalse(builder.isAmplitudeValid(Integer.MIN_VALUE));
    }

    @Test
    void rejectsLargeAmplitudes() {
        assertFalse(builder.isAmplitudeValid(5000));
        assertFalse(builder.isAmplitudeValid(10000));
        assertFalse(builder.isAmplitudeValid(Integer.MAX_VALUE));
    }

    @Test
    void acceptsValidAmplitudeRange() {
        for (int amp = 0; amp <= 4095; amp += 256) {
            assertTrue(builder.isAmplitudeValid(amp),
                "Amplitude " + amp + " should be valid");
        }
    }

    // ==================== Frequency Boundary Tests ====================

    @Test
    void acceptsAllValidFrequencies() {
        assertTrue(builder.isFrequencyValid(250));
        assertTrue(builder.isFrequencyValid(500));
        assertTrue(builder.isFrequencyValid(1000));
        assertTrue(builder.isFrequencyValid(2000));
        assertTrue(builder.isFrequencyValid(4000));
        assertTrue(builder.isFrequencyValid(8000));
    }

    @Test
    void rejectsFrequenciesNearBoundaries() {
        assertFalse(builder.isFrequencyValid(249));
        assertFalse(builder.isFrequencyValid(251));
        assertFalse(builder.isFrequencyValid(499));
        assertFalse(builder.isFrequencyValid(501));
        assertFalse(builder.isFrequencyValid(8001));
        assertFalse(builder.isFrequencyValid(7999));
    }

    @Test
    void rejectsInvalidFrequencies() {
        assertFalse(builder.isFrequencyValid(0));
        assertFalse(builder.isFrequencyValid(-1000));
        assertFalse(builder.isFrequencyValid(100));
        assertFalse(builder.isFrequencyValid(1500));
        assertFalse(builder.isFrequencyValid(3000));
        assertFalse(builder.isFrequencyValid(10000));
        assertFalse(builder.isFrequencyValid(Integer.MAX_VALUE));
    }

    // ==================== Combined Command Tests ====================

    @Test
    void rejectsCombinedCommandWithInvalidFrequency() {
        assertFalse(builder.buildCombinedCommand(999, 2048).isPresent());
        assertFalse(builder.buildCombinedCommand(1001, 2048).isPresent());
    }

    @Test
    void rejectsCombinedCommandWithInvalidAmplitude() {
        assertFalse(builder.buildCombinedCommand(1000, -1).isPresent());
        assertFalse(builder.buildCombinedCommand(1000, 4096).isPresent());
    }

    @Test
    void acceptsCombinedCommandWithValidParameters() {
        assertTrue(builder.buildCombinedCommand(1000, 0).isPresent());
        assertTrue(builder.buildCombinedCommand(1000, 2048).isPresent());
        assertTrue(builder.buildCombinedCommand(1000, 4095).isPresent());
        
        assertTrue(builder.buildCombinedCommand(250, 1024).isPresent());
        assertTrue(builder.buildCombinedCommand(8000, 3072).isPresent());
    }

    @Test
    void rejectsCombinedCommandWithBothInvalid() {
        assertFalse(builder.buildCombinedCommand(999, -1).isPresent());
        assertFalse(builder.buildCombinedCommand(1001, 4096).isPresent());
        assertFalse(builder.buildCombinedCommand(0, 0).isPresent());
    }

    // ==================== Edge Case Tests ====================

    @Test
    void handleZeroValues() {
        assertTrue(builder.isAmplitudeValid(0));
        assertFalse(builder.isFrequencyValid(0));
    }

    @Test
    void handleVeryLargeIntegers() {
        assertFalse(builder.isAmplitudeValid(Integer.MAX_VALUE));
        assertFalse(builder.isFrequencyValid(Integer.MAX_VALUE));
    }

    @Test
    void formatsCommandsCorrectly() {
        var cmd = builder.buildFrequencyCommand(1000);
        assertTrue(cmd.isPresent());
        assertTrue(cmd.get().equals("FREQ:1000"));
        
        var amp = builder.buildAmplitudeCommand(2048);
        assertTrue(amp.isPresent());
        assertTrue(amp.get().equals("AMP:2048"));
    }
}
