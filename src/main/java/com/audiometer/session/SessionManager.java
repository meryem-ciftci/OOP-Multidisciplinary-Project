package com.audiometer.session;

import com.audiometer.domain.Ear;
import com.audiometer.domain.ResponseRecord;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestPhase;
import com.audiometer.domain.TestSession;
import com.audiometer.domain.TestState;
import com.audiometer.domain.ThresholdResult;
import com.audiometer.engine.ClinicalEvent;
import com.audiometer.engine.ClinicalStateMachine;
import com.audiometer.engine.HughsonWestlakeEngine;
import com.audiometer.util.ClinicalConstants;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Orchestrates a complete clinical audiometry test session.
 *
 * <p>SessionManager acts as the workflow controller, managing the lifecycle of a hearing test
 * session. It coordinates the state machine (which defines valid state transitions) with the
 * Hughson-Westlake engine (which implements the audiometric algorithm). All test session data
 * is immutable; operations return new session instances rather than modifying existing ones.
 *
 * <p><strong>Workflow:</strong>
 * <ol>
 *   <li>Create a new session with {@link #initialize()}
 *   <li>Start the test with {@link #startTest(TestSession)}
 *   <li>For each stimulus:
 *       <ul>
 *         <li>Call {@link #playTone(TestSession)} to present stimulus
 *         <li>Call {@link #awaitResponse(TestSession)} to wait for patient response
 *         <li>Record patient's response with {@link #recordResponse(TestSession, ResponseType, Instant)}
 *         <li>Call {@link #resolveAfterProcessing(TestSession)} to check for threshold
 *       </ul>
 *   <li>If threshold found, advance with {@link #advanceAfterThreshold(TestSession)}
 *   <li>Repeat until all frequencies and ears are tested
 *   <li>Session is complete when {@link TestSession#sessionCompleted()} returns true
 * </ol>
 *
 * <p><strong>Immutability:</strong> SessionManager operates on immutable {@link TestSession}
 * records. Each operation returns a new session instance; the original session remains unchanged.
 * This enables safe concurrent access and clean audit trails.
 *
 * <p><strong>State Safety:</strong> All transitions are validated; attempting an invalid
 * operation from the wrong state throws {@link IllegalStateException}.
 *
 * @see TestSession the immutable state holder
 * @see HughsonWestlakeEngine the Hughson-Westlake algorithm implementation
 * @see ClinicalStateMachine the finite-state machine governing transitions
 */
public final class SessionManager {

    private final HughsonWestlakeEngine engine;
    private final ClinicalStateMachine stateMachine;

    public SessionManager() {
        this(new HughsonWestlakeEngine(), new ClinicalStateMachine());
    }

    public SessionManager(HughsonWestlakeEngine engine, ClinicalStateMachine stateMachine) {
        this.engine = engine;
        this.stateMachine = stateMachine;
    }

    /**
     * Initializes a new test session with default parameters.
     *
     * <p>Creates a fresh session ready for testing:
     * <ul>
     *   <li>Ear: LEFT (standard clinical order)
     *   <li>Frequency: 1000 Hz (starting frequency)
     *   <li>Level: 40 dB (initial presentation level)
     *   <li>Phase: ASCENDING (standard Hughson-Westlake ascending-run start)
     *   <li>State: INITIALIZING
     *   <li>Response history: empty
     *   <li>Detected thresholds: empty
     * </ul>
     *
     * @return a new, initialized {@link TestSession}
     */
    public TestSession initialize() {
        return new TestSession(
                Ear.LEFT,
                ClinicalConstants.START_FREQUENCY,
                ClinicalConstants.INITIAL_DB,
                TestPhase.ASCENDING,
                TestState.INITIALIZING,
                List.of(),
                List.of(),
                0,
                false);
    }

    /**
     * Transitions the session from INITIALIZING to PLAYING_TONE state, marking the start of
     * active testing.
     *
     * @param session the current session
     * @return a new session with updated state
     * @throws IllegalStateException if session is not in a state that allows starting
     */
    public TestSession startTest(TestSession session) {
        return session.withState(stateMachine.transition(session.state(), ClinicalEvent.START_SESSION));
    }

    /**
     * Ensures the session is in PLAYING_TONE state, preparing to present a stimulus to the
     * patient.
     *
     * <p>If already in PLAYING_TONE state, returns the session unchanged. Otherwise, transitions
     * to PLAYING_TONE and resets the test phase to ASCENDING.
     *
     * @param session the current session
     * @return a new session in PLAYING_TONE state with phase set to ASCENDING
     */
    public TestSession playTone(TestSession session) {
        if (session.state() == TestState.PLAYING_TONE) {
            return session;
        }
        return session.withState(stateMachine.transition(session.state(), ClinicalEvent.PLAY_TONE))
                .withCurrentPhase(TestPhase.ASCENDING);
    }

    /**
     * Transitions to WAITING_RESPONSE state, indicating the stimulus has been presented and the
     * system is now listening for patient response.
     *
     * @param session the current session
     * @return a new session in WAITING_RESPONSE state
     * @throws IllegalStateException if transition is not valid from current state
     */
    public TestSession awaitResponse(TestSession session) {
        return session.withState(stateMachine.transition(session.state(), ClinicalEvent.WAIT_FOR_RESPONSE));
    }

    /**
     * Records the patient's response to the current stimulus and advances to the next stimulus
     * level according to the Hughson-Westlake algorithm.
     *
     * <p>This method:
     * <ul>
     *   <li>Validates that the session is in WAITING_RESPONSE state
     *   <li>Creates a {@link ResponseRecord} capturing the response, frequency, level, timing, and ear
     *   <li>Treats TIMEOUT as NOT_HEARD for algorithm purposes
     *   <li>Calculates the next stimulus level using {@link HughsonWestlakeEngine#nextDb(int, ResponseType)}
     *   <li>Updates the response history and dB level
     *   <li>Transitions to PROCESSING_RESPONSE state
     * </ul>
     *
     * @param session the current session (must be in WAITING_RESPONSE or PROCESSING_RESPONSE state)
     * @param responseType the type of patient response (HEARD, NOT_HEARD, or TIMEOUT)
     * @param timestamp the time of the response
     * @return a new session with updated history, dB level, and state
     * @throws IllegalStateException if session is not in an expected response state
     */
    public TestSession recordResponse(TestSession session, ResponseType responseType, Instant timestamp) {
        if (session.state() == TestState.WAITING_RESPONSE) {
            ResponseType clinicalResponse = responseType == ResponseType.TIMEOUT ? ResponseType.NOT_HEARD : responseType;
            ResponseRecord response = new ResponseRecord(
                    session.currentFrequency(),
                    session.currentDbLevel(),
                    responseType,
                    session.currentPhase(),
                    timestamp,
                    session.currentEar());

            List<ResponseRecord> updatedHistory = append(session.responseHistory(), response);
            int nextDb = engine.nextDb(session.currentDbLevel(), clinicalResponse);

            return session.withResponseHistory(updatedHistory)
                    .withCurrentDbLevel(nextDb)
                    .withState(stateMachine.transition(session.state(), ClinicalEvent.PROCESS_RESPONSE));
        }

        if (session.state() == TestState.PROCESSING_RESPONSE && responseType == ResponseType.TIMEOUT) {
            ResponseRecord response = new ResponseRecord(
                    session.currentFrequency(),
                    session.currentDbLevel(),
                    responseType,
                    session.currentPhase(),
                    timestamp,
                    session.currentEar());

            List<ResponseRecord> updatedHistory = append(session.responseHistory(), response);
            int nextDb = engine.nextDb(session.currentDbLevel(), ResponseType.NOT_HEARD);

            return session.withResponseHistory(updatedHistory)
                    .withCurrentDbLevel(nextDb)
                    .withState(TestState.PROCESSING_RESPONSE);
        }

        throw new IllegalStateException("Expected state %s or %s but found %s".formatted(
                TestState.WAITING_RESPONSE, TestState.PROCESSING_RESPONSE, session.state()));
    }

    /**
     * After processing a response, determines whether a threshold has been reached and either
     * returns to testing or advances to the next phase.
     *
     * <p>If a threshold is detected for the current frequency and ear:
     * <ul>
     *   <li>Adds it to the thresholds list
     *   <li>Transitions to THRESHOLD_FOUND state
     * </ul>
     *
     * Otherwise, returns to PLAYING_TONE to continue testing at the current frequency.
     *
     * @param session the current session (must be in PROCESSING_RESPONSE state)
     * @return a new session either in THRESHOLD_FOUND or PLAYING_TONE state
     * @throws IllegalStateException if session is not in PROCESSING_RESPONSE state
     */
    public TestSession resolveAfterProcessing(TestSession session) {
        ensureState(session, TestState.PROCESSING_RESPONSE);

        Optional<ThresholdResult> threshold = engine.detectThreshold(session.responseHistory(), session.currentEar(), session.currentFrequency());
        if (threshold.isPresent()) {
            List<ThresholdResult> updatedThresholds = append(session.thresholds(), threshold.orElseThrow());
            return session.withThresholds(updatedThresholds).withState(TestState.THRESHOLD_FOUND);
        }

        return session.withState(TestState.PLAYING_TONE);
    }

    /**
     * After a threshold is found, advances to the next frequency or ear following the standard
     * clinical protocol.
     *
     * <p>Advancement order:
     * <ol>
     *   <li>Test left ear at all 6 frequencies
     *   <li>Then test right ear at all 6 frequencies
     *   <li>After right ear at highest frequency, test is complete
     * </ol>
     *
     * <p>When advancing to a new frequency or ear, resets the dB level to the initial value
     * (40 dB) and phase to ASCENDING.
     *
     * @param session the current session (must be in THRESHOLD_FOUND state)
     * @return a new session either in NEXT_FREQUENCY or NEXT_EAR state, or TEST_FINISHED when all
     *     testing is complete
     * @throws IllegalStateException if session is not in THRESHOLD_FOUND state
     */
    public TestSession advanceAfterThreshold(TestSession session) {
        ensureState(session, TestState.THRESHOLD_FOUND);

        if (session.currentEar() == Ear.RIGHT && session.frequencyIndex() == ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1) {
            return session.withState(TestState.TEST_FINISHED).withSessionCompleted(true);
        }

        if (session.frequencyIndex() == ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1) {
            TestSession next = session.withCurrentEar(Ear.RIGHT)
                    .withCurrentFrequency(ClinicalConstants.ALLOWED_FREQUENCIES.get(0))
                    .withCurrentDbLevel(ClinicalConstants.INITIAL_DB)
                    .withFrequencyIndex(0)
                    .withCurrentPhase(TestPhase.ASCENDING)
                    .withState(stateMachine.transition(TestState.THRESHOLD_FOUND, ClinicalEvent.ADVANCE_EAR));
            return next;
        }

        int nextIndex = engine.nextFrequencyIndex(session.frequencyIndex());
        return session.withCurrentFrequency(ClinicalConstants.ALLOWED_FREQUENCIES.get(nextIndex))
                .withCurrentDbLevel(ClinicalConstants.INITIAL_DB)
                .withFrequencyIndex(nextIndex)
                .withCurrentPhase(TestPhase.ASCENDING)
                .withState(stateMachine.transition(TestState.THRESHOLD_FOUND, ClinicalEvent.ADVANCE_FREQUENCY));
    }

    /**
     * Handles a timeout (patient did not respond within the allowed time window).
     *
     * <p>Timeouts are recorded as responses and treated as NOT_HEARD for algorithm purposes,
     * following conservative clinical practice. This method is safe to call while the session is
     * already in {@link TestState#PROCESSING_RESPONSE}, allowing repeated timeout cycles to be
     * recorded without invalid state transitions.
     *
     * @param session the current session
     * @param timestamp the time at which the timeout occurred
     * @return a new session with the timeout recorded
     */
    public TestSession handleTimeout(TestSession session, Instant timestamp) {
        return recordResponse(session, ResponseType.TIMEOUT, timestamp);
    }

    /**
     * Validates that the session is in the expected state; throws an exception if not.
     *
     * @param session the session to check
     * @param expected the expected state
     * @throws IllegalStateException if the session is not in the expected state
     */
    private void ensureState(TestSession session, TestState expected) {
        if (session.state() != expected) {
            throw new IllegalStateException("Expected state %s but found %s".formatted(expected, session.state()));
        }
    }

    /**
     * Appends an element to an immutable list, returning a new list.
     *
     * @param <T> the element type
     * @param values the existing list
     * @param next the element to append
     * @return a new list containing all elements from {@code values} followed by {@code next}
     */
    private static <T> List<T> append(List<T> values, T next) {
        return Stream.concat(values.stream(), Stream.of(next)).toList();
    }
}
