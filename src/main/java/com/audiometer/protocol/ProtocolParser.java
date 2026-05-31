package com.audiometer.protocol;

import java.util.Optional;

/**
 * Parses messages from the clinical audiometer device.
 *
 * <p>The protocol parser implements a defensive, Optional-based parsing strategy for incoming
 * messages from the serial device. It safely handles malformed input and normalization, returning
 * parsed {@link DeviceMessage} instances or empty when parsing fails.
 *
 * <p><strong>Protocol Messages:</strong>
 * <ul>
 *   <li>{@code READY} - Device is initialized and ready for commands
 *   <li>{@code RESPONSE} - Patient has responded to the current stimulus
 *   <li>{@code ACK:<command>} - Device acknowledged a command (e.g., ACK:FREQ:1000)
 *   <li>{@code NAK:<reason>} - Device rejected a command (FREQ_OUT_OF_RANGE, AMP_OUT_OF_RANGE,
 *       UNKNOWN_CMD)
 * </ul>
 *
 * <p><strong>Parsing Safety:</strong>
 * <ul>
 *   <li>Null input returns empty
 *   <li>Blank input returns empty
 *   <li>Unrecognized messages return empty
 *   <li>Malformed ACK/NAK returns empty
 *   <li>Case-insensitive parsing (input normalized to uppercase)
 * </ul>
 *
 * <p><strong>Immutability:</strong> This class is stateless and immutable.
 */
public final class ProtocolParser {

    /**
     * Parses a raw message line from the serial device.
     *
     * <p>This method applies defensive parsing:
     * <ol>
     *   <li>Checks for null input
     *   <li>Normalizes whitespace and case
     *   <li>Matches against known message patterns
     *   <li>Returns empty for any unrecognized or malformed input
     * </ol>
     *
     * @param rawLine the raw message string from the device
     * @return an {@link Optional} containing the parsed {@link DeviceMessage}, or empty if parsing
     *     fails
     */
    public Optional<DeviceMessage> parse(String rawLine) {
        if (rawLine == null) {
            return Optional.empty();
        }

        String normalized = rawLine.trim().toUpperCase();
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        if (normalized.equals("READY")) {
            return Optional.of(new ReadyMessage());
        }

        if (normalized.equals("RESPONSE")) {
            return Optional.of(new ResponseMessage());
        }

        if (normalized.startsWith("ACK:")) {
            String command = normalized.substring(4).trim();
            if (command.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AckMessage(command));
        }

        if (normalized.startsWith("NAK:")) {
            String reason = normalized.substring(4).trim();
            return parseNak(reason);
        }

        return Optional.empty();
    }

    /**
     * Parses NAK (negative acknowledgment) reason codes.
     *
     * @param reason the NAK reason string (should be one of: FREQ_OUT_OF_RANGE, AMP_OUT_OF_RANGE,
     *     UNKNOWN_CMD)
     * @return an {@link Optional} containing a {@link NakMessage} if the reason is recognized, or
     *     empty otherwise
     */
    private Optional<DeviceMessage> parseNak(String reason) {
        return switch (reason) {
            case "FREQ_OUT_OF_RANGE" -> Optional.of(new NakMessage(NakReason.FREQ_OUT_OF_RANGE));
            case "AMP_OUT_OF_RANGE" -> Optional.of(new NakMessage(NakReason.AMP_OUT_OF_RANGE));
            case "UNKNOWN_CMD" -> Optional.of(new NakMessage(NakReason.UNKNOWN_CMD));
            default -> Optional.empty();
        };
    }
}
