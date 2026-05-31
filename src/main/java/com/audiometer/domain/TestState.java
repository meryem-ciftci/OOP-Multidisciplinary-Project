package com.audiometer.domain;

public enum TestState {
    IDLE,
    INITIALIZING,
    PLAYING_TONE,
    WAITING_RESPONSE,
    PROCESSING_RESPONSE,
    THRESHOLD_FOUND,
    NEXT_FREQUENCY,
    NEXT_EAR,
    TEST_FINISHED,
    ERROR
}
