package com.audiometer.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Provide;
import net.jqwik.api.Property;
import net.jqwik.api.providers.TypeUsage;
import org.junit.jupiter.api.Test;

/**
 * Robustness tests for protocol parsing with malformed and edge-case inputs.
 *
 * <p>These tests ensure that the protocol parser never crashes on malformed input and always
 * returns safe Optional.empty() results.
 */
class ProtocolRobustnessTest {

    private final ProtocolParser parser = new ProtocolParser();

    @Test
    void parsesNullGracefully() {
        assertTrue(parser.parse(null).isEmpty());
    }

    @Test
    void parsesEmptyStringGracefully() {
        assertTrue(parser.parse("").isEmpty());
    }

    @Test
    void parsesBlankStringGracefully() {
        assertTrue(parser.parse("   ").isEmpty());
        assertTrue(parser.parse("\t\n").isEmpty());
    }

    @Test
    void parsesRandomGarbageGracefully() {
        assertTrue(parser.parse("xyz123!@#$%^&*()").isEmpty());
        assertTrue(parser.parse("GARBAGE").isEmpty());
        assertTrue(parser.parse("not a real message").isEmpty());
    }

    @Test
    void parsesAckWithEmptyCommandGracefully() {
        assertTrue(parser.parse("ACK:").isEmpty());
        assertTrue(parser.parse("ACK:   ").isEmpty());
    }

    @Test
    void parsesNakWithEmptyReasonGracefully() {
        assertTrue(parser.parse("NAK:").isEmpty());
        assertTrue(parser.parse("NAK:   ").isEmpty());
    }

    @Test
    void parsesNakWithInvalidReasonGracefully() {
        assertTrue(parser.parse("NAK:INVALID_REASON").isEmpty());
        assertTrue(parser.parse("NAK:UNKNOWN_ERROR").isEmpty());
    }

    @Test
    void parsesExtraWhitespaceGracefully() {
        assertTrue(parser.parse("   READY   ").isPresent());
        assertTrue(parser.parse("ACK:   START   ").isPresent());
    }

    @Test
    void parsesLowercaseMessagesGracefully() {
        assertTrue(parser.parse("ready").isPresent());
        assertTrue(parser.parse("response").isPresent());
        assertTrue(parser.parse("ack:start").isPresent());
        assertTrue(parser.parse("nak:freq_out_of_range").isPresent());
    }

    @Test
    void parsesVeryLongInputGracefully() {
        String longString = "A".repeat(10000);
        assertTrue(parser.parse(longString).isEmpty());
    }

    @Test
    void parsesMalformedAckVariantsGracefully() {
        assertTrue(parser.parse("ACK::command").isPresent()); // ACK with empty command then more
        assertTrue(parser.parse("ack:FREQ:1000,AMP:40").isPresent()); // Full command in ACK
    }

    @Test
    void parsesMultilineInputAsEmpty() {
        assertTrue(parser.parse("READY\nRESPONSE").isEmpty()); // Multiple lines
    }

    @Test
    void parsesSpecialCharactersGracefully() {
        assertTrue(parser.parse("ACK:FREQ:null").isPresent()); // Unusual but valid command
        assertTrue(parser.parse("NAK:ERROR_[FREQ]").isEmpty()); // Invalid reason format
    }

    @Test
    void parsesRepeatedPrefixesGracefully() {
        assertTrue(parser.parse("ACK:ACK:START").isPresent());
        assertTrue(parser.parse("NAK:NAK:FREQ_OUT_OF_RANGE").isEmpty()); // NAK reason invalid
    }
}
