package com.audiometer.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProtocolParserTest {

    private final ProtocolParser parser = new ProtocolParser();

    @Test
    void parsesReadyMessage() {
        assertInstanceOf(ReadyMessage.class, parser.parse("READY").orElseThrow());
    }

    @Test
    void parsesAcknowledgementAndNakMessages() {
        assertEquals("START", ((AckMessage) parser.parse("ACK:START").orElseThrow()).command());
        assertEquals(NakReason.FREQ_OUT_OF_RANGE, ((NakMessage) parser.parse("NAK:FREQ_OUT_OF_RANGE").orElseThrow()).reason());
    }

    @Test
    void parsesResponseMessages() {
        assertInstanceOf(ResponseMessage.class, parser.parse("RESPONSE").orElseThrow());
    }

    @Test
    void rejectsMalformedInputSafely() {
        assertTrue(parser.parse("some invalid line").isEmpty());
    }
}
