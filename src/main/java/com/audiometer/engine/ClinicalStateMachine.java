package com.audiometer.engine;

import com.audiometer.domain.TestState;
import com.audiometer.state.StateTransition;

/**
 * Deterministic finite-state machine for clinical audiometry test workflow.
 *
 * <p>This class models the behavior of a clinical audiometer test session using a finite state
 * machine pattern. Each state represents a distinct phase in the Hughson-Westlake threshold
 * detection workflow, and transitions between states are strictly controlled by valid clinical
 * events.
 *
 * <p><strong>State Workflow:</strong>
 * <ul>
 *   <li>{@link TestState#IDLE} - Initial state, no test active
 *   <li>{@link TestState#INITIALIZING} - Session initialization in progress
 *   <li>{@link TestState#PLAYING_TONE} - Tone stimulus is being presented to patient
 *   <li>{@link TestState#WAITING_RESPONSE} - Waiting for patient response to stimulus
 *   <li>{@link TestState#PROCESSING_RESPONSE} - Processing patient's response and determining next step
 *   <li>{@link TestState#THRESHOLD_FOUND} - Threshold detected for current frequency and ear
 *   <li>{@link TestState#NEXT_FREQUENCY} - Advancing to next test frequency
 *   <li>{@link TestState#NEXT_EAR} - Advancing to test the other ear
 *   <li>{@link TestState#TEST_FINISHED} - All frequencies tested for both ears
 *   <li>{@link TestState#ERROR} - An error condition occurred
 * </ul>
 *
 * <p><strong>Invariants:</strong>
 * <ul>
 *   <li>Transitions are deterministic: only specific events are valid from each state</li>
 *   <li>Invalid transitions throw {@link IllegalStateException}</li>
 *   <li>State machine is immutable; no state is retained between transitions</li>
 *   <li>All transitions are synchronous and atomic</li>
 * </ul>
 *
 * <p>Usage: This class should be used with {@link HughsonWestlakeEngine} and
 * {@link com.audiometer.session.SessionManager} to orchestrate a complete test session.
 */
public final class ClinicalStateMachine {

    /**
     * Validates whether a transition from the given state on the given event is allowed.
     *
     * <p>This method implements the transition rules of the state machine without changing any
     * state. It can be used to check validity before attempting a transition.
     *
     * @param current the current state
     * @param event the event that would trigger a transition
     * @return {@code true} if the transition is valid, {@code false} otherwise
     */
    public boolean isValidTransition(TestState current, ClinicalEvent event) {
        return switch (current) {
            case IDLE -> event == ClinicalEvent.START_SESSION;
            case INITIALIZING -> event == ClinicalEvent.START_SESSION || event == ClinicalEvent.ERROR;
            case PLAYING_TONE -> event == ClinicalEvent.WAIT_FOR_RESPONSE;
            case WAITING_RESPONSE -> event == ClinicalEvent.PROCESS_RESPONSE || event == ClinicalEvent.ERROR;
            case PROCESSING_RESPONSE -> event == ClinicalEvent.PLAY_TONE || event == ClinicalEvent.THRESHOLD_FOUND;
            case THRESHOLD_FOUND -> event == ClinicalEvent.ADVANCE_FREQUENCY || event == ClinicalEvent.ADVANCE_EAR;
            case NEXT_FREQUENCY -> event == ClinicalEvent.PLAY_TONE;
            case NEXT_EAR -> event == ClinicalEvent.PLAY_TONE;
            case TEST_FINISHED -> event == ClinicalEvent.RESET;
            case ERROR -> event == ClinicalEvent.RESET;
        };
    }

    /**
     * Performs a transition and returns a {@link StateTransition} record capturing the event
     * details.
     *
     * <p>This method validates the transition first and throws an exception if invalid.
     * The transition logic follows the Hughson-Westlake protocol:
     *
     * <ul>
     *   <li>From PROCESSING_RESPONSE: advances to THRESHOLD_FOUND if threshold detected, otherwise
     *       returns to PLAYING_TONE to continue testing at current frequency
     *   <li>From THRESHOLD_FOUND: determines whether to advance to next frequency or next ear based
     *       on the event
     * </ul>
     *
     * @param current the current state
     * @param event the clinical event to process
     * @return a {@link StateTransition} record containing the transition metadata
     * @throws IllegalStateException if the transition is not valid from the current state
     */
    public StateTransition transitionRecord(TestState current, ClinicalEvent event) {
        if (!isValidTransition(current, event)) {
            throw new IllegalStateException("Invalid transition from %s on %s".formatted(current, event));
        }

        TestState next = switch (current) {
            case IDLE -> TestState.INITIALIZING;
            case INITIALIZING -> TestState.PLAYING_TONE;
            case PLAYING_TONE -> TestState.WAITING_RESPONSE;
            case WAITING_RESPONSE -> TestState.PROCESSING_RESPONSE;
            case PROCESSING_RESPONSE -> event == ClinicalEvent.THRESHOLD_FOUND ? TestState.THRESHOLD_FOUND : TestState.PLAYING_TONE;
            case THRESHOLD_FOUND -> event == ClinicalEvent.ADVANCE_FREQUENCY ? TestState.NEXT_FREQUENCY : TestState.NEXT_EAR;
            case NEXT_FREQUENCY -> TestState.PLAYING_TONE;
            case NEXT_EAR -> TestState.PLAYING_TONE;
            case TEST_FINISHED -> TestState.IDLE;
            case ERROR -> TestState.IDLE;
        };

        return new StateTransition(current, next, event);
    }

    /**
     * Performs a state transition given the current state and the triggering event.
     *
     * <p>This is the primary method for advancing the state machine. It is a convenience method
     * that extracts the target state from the transition record.
     *
     * @param current the current state
     * @param event the clinical event to process
     * @return the next state
     * @throws IllegalStateException if the transition is not valid from the current state
     */
    public TestState transition(TestState current, ClinicalEvent event) {
        return transitionRecord(current, event).to();
    }
}
