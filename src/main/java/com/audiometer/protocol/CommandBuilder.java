package com.audiometer.protocol;

import com.audiometer.util.ClinicalConstants;
import java.util.Optional;

/**
 * Builds valid command strings for the clinical audiometer device.
 *
 * <p>This class implements a Optional-based command builder that validates command parameters
 * before generating protocol strings. It ensures that all commands sent to the device comply with
 * the serial protocol specification, preventing invalid stimulus parameters.
 *
 * <p><strong>Protocol Specification:</strong>
 * <ul>
 *   <li>{@code PING} - Test device connectivity
 *   <li>{@code START} - Begin audio stimulus presentation
 *   <li>{@code STOP} - Stop audio stimulus immediately
 *   <li>{@code FREQ:<frequency>} - Set test frequency in Hz (must be in allowed list)
 *   <li>{@code AMP:<amplitude>} - Set DAC amplitude (0-4095)
 *   <li>{@code FREQ:<frequency>,AMP:<amplitude>} - Combined frequency and amplitude command
 * </ul>
 *
 * <p><strong>Validation Rules:</strong>
 * <ul>
 *   <li>Frequency must be in {@link ClinicalConstants#ALLOWED_FREQUENCIES}
 *   <li>Amplitude must be in range [0, 4095] (DAC amplitude, NOT clinical dB)
 * </ul>
 *
 * <p><strong>Immutability:</strong> This class is stateless and immutable.
 */
public final class CommandBuilder {

    /**
     * Builds a PING command to test device connectivity.
     *
     * @return an {@link Optional} containing "PING"
     */
    public Optional<String> buildPingCommand() {
        return Optional.of("PING");
    }

    /**
     * Builds a START command to begin audio stimulus presentation.
     *
     * @return an {@link Optional} containing "START"
     */
    public Optional<String> buildStartCommand() {
        return Optional.of("START");
    }

    /**
     * Builds a STOP command to halt audio stimulus.
     *
     * @return an {@link Optional} containing "STOP"
     */
    public Optional<String> buildStopCommand() {
        return Optional.of("STOP");
    }

    /**
     * Builds a FREQ command to set the test frequency.
     *
     * @param frequency the frequency in Hz
     * @return an {@link Optional} containing the formatted command "FREQ:<frequency>", or empty if
     *     the frequency is invalid
     */
    public Optional<String> buildFrequencyCommand(int frequency) {
        if (!isFrequencyValid(frequency)) {
            return Optional.empty();
        }
        return Optional.of("FREQ:%d".formatted(frequency));
    }

    /**
     * Builds an AMP command to set the DAC amplitude.
     *
     * <p><strong>Important:</strong> Amplitude (0-4095) is a DAC control value, NOT a clinical dB
     * level. This is distinct from the clinical stimulus level which is calculated separately from
     * amplitude and other device parameters.
     *
     * @param amplitude the DAC amplitude (0-4095)
     * @return an {@link Optional} containing the formatted command "AMP:<amplitude>", or empty if
     *     the amplitude is invalid
     */
    public Optional<String> buildAmplitudeCommand(int amplitude) {
        if (!isAmplitudeValid(amplitude)) {
            return Optional.empty();
        }
        return Optional.of("AMP:%d".formatted(amplitude));
    }

    /**
     * Builds a combined FREQ and AMP command in a single message.
     *
     * @param frequency the frequency in Hz
     * @param amplitude the DAC amplitude (0-4095)
     * @return an {@link Optional} containing the formatted command "FREQ:<frequency>,AMP:<amplitude>",
     *     or empty if either parameter is invalid
     */
    public Optional<String> buildCombinedCommand(int frequency, int amplitude) {
        if (!isFrequencyValid(frequency) || !isAmplitudeValid(amplitude)) {
            return Optional.empty();
        }
        return Optional.of("FREQ:%d,AMP:%d".formatted(frequency, amplitude));
    }

    /**
     * Validates that a frequency is in the allowed clinical range.
     *
     * @param frequency the frequency in Hz
     * @return true if the frequency is in {@link ClinicalConstants#ALLOWED_FREQUENCIES}
     */
    public boolean isFrequencyValid(int frequency) {
        return ClinicalConstants.ALLOWED_FREQUENCIES.contains(frequency);
    }

    /**
     * Validates that an amplitude is in the valid DAC range.
     *
     * <p><strong>Note:</strong> This validates DAC amplitude (0-4095), NOT clinical dB levels
     * (0-120). These are separate domains in the serial protocol.
     *
     * @param amplitude the DAC amplitude value
     * @return true if the amplitude is in range [0, 4095]
     */
    public boolean isAmplitudeValid(int amplitude) {
        return amplitude >= ClinicalConstants.MIN_AMPLITUDE && amplitude <= ClinicalConstants.MAX_AMPLITUDE;
    }
}
