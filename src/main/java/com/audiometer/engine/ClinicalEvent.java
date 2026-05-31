package com.audiometer.engine;

public enum ClinicalEvent {
    START_SESSION,
    PLAY_TONE,
    WAIT_FOR_RESPONSE,
    PROCESS_RESPONSE,
    THRESHOLD_FOUND,
    ADVANCE_FREQUENCY,
    ADVANCE_EAR,
    RESET,
    ERROR
}
