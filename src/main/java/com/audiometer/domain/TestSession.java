package com.audiometer.domain;

import java.util.List;

/**
 * Immutable record representing the complete state of a clinical audiometry test session.
 *
 * <p>TestSession is the central data structure that tracks all aspects of a hearing test:
 * <ul>
 *   <li>Current ear and frequency being tested
 *   <li>Current stimulus level (in clinical dB)
 *   <li>Current test phase (ascending or descending run)
 *   <li>State machine state
 *   <li>Complete history of patient responses
 *   <li>All detected thresholds
 * </ul>
 *
 * <p><strong>Immutability:</strong> TestSession is a Java record and is fully immutable.
 * <ul>
 *   <li>All list fields are defensively copied to unmodifiable lists
 *   <li>No setters exist; use {@code with*} methods to create modified copies
 *   <li>Safe for concurrent access and audit trails
 * </ul>
 *
 * <p><strong>Invariants:</strong>
 * <ul>
 *   <li>{@code currentDbLevel} is always in range [0, 120]
 *   <li>{@code currentFrequency} must be in {@link ClinicalConstants#ALLOWED_FREQUENCIES}
 *   <li>{@code frequencyIndex} is always a valid index into ALLOWED_FREQUENCIES
 *   <li>{@code responseHistory} and {@code thresholds} are never null; may be empty
 *   <li>No field is null except as explicitly documented
 * </ul>
 *
 * <p><strong>Design Pattern:</strong> Uses the builder pattern via {@code with*} methods.
 * Each method returns a new TestSession with one field modified, leaving the original unchanged:
 * <pre>
 *   TestSession updated = session.withCurrentDbLevel(50).withState(TestState.PLAYING_TONE);
 * </pre>
 *
 * @param currentEar the ear currently being tested (LEFT or RIGHT)
 * @param currentFrequency the frequency currently being tested, in Hz
 * @param currentDbLevel the current stimulus level in clinical dB (0-120)
 * @param currentPhase the current phase of testing (ASCENDING or DESCENDING)
 * @param state the current state in the test workflow
 * @param responseHistory an immutable list of all responses recorded so far
 * @param thresholds an immutable list of all thresholds detected so far
 * @param frequencyIndex the current index into {@link ClinicalConstants#ALLOWED_FREQUENCIES}
 * @param sessionCompleted true if all required testing has been completed
 */
public record TestSession(
        Ear currentEar,
        int currentFrequency,
        int currentDbLevel,
        TestPhase currentPhase,
        TestState state,
        List<ResponseRecord> responseHistory,
        List<ThresholdResult> thresholds,
        int frequencyIndex,
        boolean sessionCompleted) {

    /**
     * Compact constructor that enforces immutability by copying list parameters.
     */
    public TestSession {
        responseHistory = List.copyOf(responseHistory);
        thresholds = List.copyOf(thresholds);
    }

    /**
     * Returns a new TestSession with the state updated.
     *
     * @param state the new state
     * @return a new TestSession with the updated state
     */
    public TestSession withState(TestState state) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the response history updated.
     *
     * @param responseHistory the new response history
     * @return a new TestSession with the updated response history
     */
    public TestSession withResponseHistory(List<ResponseRecord> responseHistory) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the detected thresholds updated.
     *
     * @param thresholds the new list of detected thresholds
     * @return a new TestSession with the updated thresholds
     */
    public TestSession withThresholds(List<ThresholdResult> thresholds) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the current stimulus level updated.
     *
     * @param currentDbLevel the new stimulus level in clinical dB
     * @return a new TestSession with the updated dB level
     */
    public TestSession withCurrentDbLevel(int currentDbLevel) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the test phase updated.
     *
     * @param currentPhase the new test phase
     * @return a new TestSession with the updated phase
     */
    public TestSession withCurrentPhase(TestPhase currentPhase) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the current ear updated.
     *
     * @param currentEar the new ear
     * @return a new TestSession with the updated ear
     */
    public TestSession withCurrentEar(Ear currentEar) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the current frequency updated.
     *
     * @param currentFrequency the new frequency in Hz
     * @return a new TestSession with the updated frequency
     */
    public TestSession withCurrentFrequency(int currentFrequency) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the frequency index updated.
     *
     * @param frequencyIndex the new index
     * @return a new TestSession with the updated frequency index
     */
    public TestSession withFrequencyIndex(int frequencyIndex) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }

    /**
     * Returns a new TestSession with the session completion flag updated.
     *
     * @param sessionCompleted true if the session is complete
     * @return a new TestSession with the updated completion flag
     */
    public TestSession withSessionCompleted(boolean sessionCompleted) {
        return new TestSession(currentEar, currentFrequency, currentDbLevel, currentPhase, state, responseHistory, thresholds,
                frequencyIndex, sessionCompleted);
    }
}
