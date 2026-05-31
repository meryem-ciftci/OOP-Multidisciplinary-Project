package com.audiometer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestSession;
import com.audiometer.domain.TestState;
import com.audiometer.protocol.ProtocolParser;
import com.audiometer.session.SessionManager;
import java.time.Instant;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;

class ClinicalPropertiesTest {

    private final ProtocolParser parser = new ProtocolParser();

    @Property
    void parserNeverThrowsOnRandomInput(@ForAll String input) {
        assertDoesNotThrow(() -> parser.parse(input));
    }

    @Property
    void dbLevelRemainsWithinBounds(@ForAll @Size(min = 1, max = 18) List<ResponseType> responses) {
        SessionManager manager = new SessionManager();
        TestSession session = manager.initialize();
        session = manager.startTest(session);
        session = manager.awaitResponse(session);

        for (ResponseType response : responses) {
            session = manager.recordResponse(session, response, Instant.EPOCH.plusSeconds(session.responseHistory().size() + 1L));
            session = manager.resolveAfterProcessing(session);

            if (session.state() == TestState.TEST_FINISHED) {
                break;
            }

            if (session.state() == TestState.THRESHOLD_FOUND) {
                session = manager.advanceAfterThreshold(session);
            }

            session = manager.playTone(session);
            session = manager.awaitResponse(session);
        }

        assertTrue(session.currentDbLevel() >= 0 && session.currentDbLevel() <= 120);
    }

    @Property
    void responseHistoryIsImmutable(@ForAll @Size(min = 1, max = 6) List<ResponseType> responses) {
        SessionManager manager = new SessionManager();
        TestSession session = manager.initialize();
        session = manager.startTest(session);
        session = manager.awaitResponse(session);

        for (ResponseType response : responses) {
            session = manager.recordResponse(session, response, Instant.EPOCH.plusSeconds(session.responseHistory().size() + 1L));
            session = manager.resolveAfterProcessing(session);
            if (session.state() == TestState.THRESHOLD_FOUND) {
                session = manager.advanceAfterThreshold(session);
            }
            session = manager.playTone(session);
            session = manager.awaitResponse(session);
        }

        TestSession immutableSession = session;
        assertThrows(UnsupportedOperationException.class, () -> immutableSession.responseHistory().add(null));
    }
}
