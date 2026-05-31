package com.audiometer.engine;

import com.audiometer.domain.Ear;
import com.audiometer.domain.ResponseRecord;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestSession;
import com.audiometer.domain.TestState;
import com.audiometer.domain.ThresholdResult;
import com.audiometer.error.ClinicalError;
import com.audiometer.util.ClinicalConstants;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the Hughson-Westlake clinical audiometry threshold detection algorithm.
 *
 * <p>The Hughson-Westlake method is the standard clinical procedure for pure-tone threshold
 * testing in audiometry. This engine encapsulates the algorithmic rules that govern how stimulus
 * levels (in dB) are adjusted based on patient responses, allowing the clinician to efficiently
 * identify the hearing threshold at each test frequency.
 *
 * <p><strong>Algorithm Overview:</strong>
 * <ul>
 *   <li>Start at an initial presentation level (typically 40 dB)
 *   <li>If patient responds (HEARD), decrease stimulus level by {@link ClinicalConstants#HEARD_STEP}
 *       (10 dB) and repeat
 *   <li>If patient does not respond (NOT_HEARD), increase stimulus level by
 *       {@link ClinicalConstants#NOT_HEARD_STEP} (5 dB) and repeat
 *   <li>Threshold is determined when a pattern of responses emerges (typically defined as 2-3
 *       reversals within 10 dB range)
 *   <li>Process repeats for each of 6 standard frequencies (250-8000 Hz) in both ears
 * </ul>
 *
 * <p><strong>Immutability:</strong> This class is stateless and immutable. All methods are
 * pure functions that accept parameters and return results without side effects.
 *
 * <p><strong>Invariants:</strong>
 * <ul>
 *   <li>dB levels are clamped to {@link ClinicalConstants#MIN_DB} to {@link ClinicalConstants#MAX_DB}
 *   <li>Only standard frequencies defined in {@link ClinicalConstants#ALLOWED_FREQUENCIES} are valid
 *   <li>Transitions follow the state machine defined by {@link ClinicalStateMachine}
 * </ul>
 *
 * <p>Usage: Typically instantiated by {@link com.audiometer.session.SessionManager}; the engine
 * handles all algorithmic decisions while the session manager handles workflow orchestration.
 */
public final class HughsonWestlakeEngine {

    private final ThresholdDetector thresholdDetector;
    private final ClinicalStateMachine stateMachine;

    public HughsonWestlakeEngine() {
        this(new ThresholdDetector(), new ClinicalStateMachine());
    }

    public HughsonWestlakeEngine(ThresholdDetector thresholdDetector, ClinicalStateMachine stateMachine) {
        this.thresholdDetector = thresholdDetector;
        this.stateMachine = stateMachine;
    }

    /**
     * Calculates the next dB level based on the patient's response.
     *
     * <p>This implements the core Hughson-Westlake algorithm:
     * <ul>
     *   <li>If patient HEARD the tone: decrease level by 10 dB (descending run)
     *   <li>If patient NOT_HEARD or TIMEOUT: increase level by 5 dB (ascending run)
     * </ul>
     *
     * <p>The resulting level is clamped to the clinical valid range [0 dB, 120 dB].
     *
     * @param currentDbLevel the current stimulus level in dB (0-120)
     * @param responseType the patient's response to the stimulus
     * @return the next stimulus level in dB, clamped to valid range
     */
    public int nextDb(int currentDbLevel, ResponseType responseType) {
        int delta = responseType == ResponseType.HEARD ? -ClinicalConstants.HEARD_STEP : ClinicalConstants.NOT_HEARD_STEP;
        return Math.max(ClinicalConstants.MIN_DB, Math.min(ClinicalConstants.MAX_DB, currentDbLevel + delta));
    }

    /**
     * Detects whether a hearing threshold has been established for the given ear at the given
     * frequency.
     *
     * <p>Threshold detection uses response history and applies clinically validated criteria (e.g.,
     * reversals within acceptable range). This method delegates to {@link ThresholdDetector}.
     *
     * @param history the list of response records from the current test session
     * @param ear the ear being tested
     * @param frequency the test frequency in Hz
     * @return an {@link Optional} containing the detected threshold, or empty if threshold has not
     *     yet been established
     */
    public Optional<ThresholdResult> detectThreshold(List<ResponseRecord> history, Ear ear, int frequency) {
        return thresholdDetector.detectThreshold(history, ear, frequency);
    }

    /**
     * Advances to the next frequency index in the standard audiometry sequence.
     *
     * <p>Does not wrap; index is clamped to the maximum valid frequency index.
     *
     * @param currentIndex the current index into {@link ClinicalConstants#ALLOWED_FREQUENCIES}
     * @return the next index, or the current index if already at the last frequency
     */
    public int nextFrequencyIndex(int currentIndex) {
        return Math.min(currentIndex + 1, ClinicalConstants.ALLOWED_FREQUENCIES.size() - 1);
    }

    /**
     * Advances to the next ear in the testing protocol (LEFT ↔ RIGHT).
     *
     * @param currentEar the current ear being tested
     * @return LEFT if currentEar is RIGHT; RIGHT if currentEar is LEFT
     */
    public Ear nextEar(Ear currentEar) {
        return currentEar == Ear.LEFT ? Ear.RIGHT : Ear.LEFT;
    }

    /**
     * Performs a state transition in the audiometry workflow.
     *
     * @param currentState the current state of the test session
     * @param event the clinical event triggering the transition
     * @return the next state
     * @throws IllegalStateException if the transition is not valid from the current state
     */
    public TestState transitionState(TestState currentState, ClinicalEvent event) {
        return stateMachine.transition(currentState, event);
    }

    /**
     * Validates that a test session satisfies all clinical invariants.
     *
     * <p>This method performs sanity checks on the session state to ensure it conforms to clinical
     * requirements:
     * <ul>
     *   <li>Current frequency is one of the standard allowed frequencies
     *   <li>Current dB level is within the valid range [0, 120]
     *   <li>No critical fields (ear, phase, state) are null
     * </ul>
     *
     * @param session the test session to validate
     * @return an {@link Optional} containing a {@link ClinicalError} if validation fails, or empty
     *     if the session is valid
     */
    public Optional<ClinicalError> validateClinicalRules(TestSession session) {
        if (!ClinicalConstants.ALLOWED_FREQUENCIES.contains(session.currentFrequency())) {
            return Optional.of(new ClinicalError("Current frequency is outside the allowed clinical range."));
        }
        if (session.currentDbLevel() < ClinicalConstants.MIN_DB || session.currentDbLevel() > ClinicalConstants.MAX_DB) {
            return Optional.of(new ClinicalError("Current dB level is outside the allowed clinical range."));
        }
        if (session.currentEar() == null || session.currentPhase() == null || session.state() == null) {
            return Optional.of(new ClinicalError("Session contains a null clinical field."));
        }
        return Optional.empty();
    }
}
