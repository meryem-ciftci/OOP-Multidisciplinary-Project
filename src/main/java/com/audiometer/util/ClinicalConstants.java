package com.audiometer.util;

import java.util.List;

public final class ClinicalConstants {

    // Clinical dB level range (attenuation levels)
    public static final int MIN_DB = 0;
    public static final int MAX_DB = 120;

    // DAC amplitude range (serial protocol AMP field: 0-4095)
    public static final int MIN_AMPLITUDE = 0;
    public static final int MAX_AMPLITUDE = 4095;

    // Test frequencies (Hz)
    public static final List<Integer> ALLOWED_FREQUENCIES = List.of(250, 500, 1000, 2000, 4000, 8000);
    public static final int START_FREQUENCY = 1000;

    // Algorithm parameters
    public static final int INITIAL_DB = 40;
    public static final int HEARD_STEP = 10;
    public static final int NOT_HEARD_STEP = 5;

    // Timeout for device response
    public static final long TIMEOUT_MILLIS = 2000L;

    private ClinicalConstants() {
    }
}
