package com.audiometer.domain;

import java.time.Instant;

/**
 * Immutable record of a patient response to a single stimulus presentation.
 *
 * <p>ResponseRecord captures all relevant details of a single test event: what was presented, how
 * the patient responded, and when it occurred. These records form the basis for threshold
 * detection and overall hearing assessment.
 *
 * <p><strong>Immutability:</strong> ResponseRecord is a Java record and is fully immutable.
 *
 * <p><strong>Clinical Significance:</strong>
 * <ul>
 *   <li>Frequency and dbLevel uniquely identify the stimulus parameters
 *   <li>ResponseType indicates the patient's perception (HEARD, NOT_HEARD, TIMEOUT)
 *   <li>Phase indicates whether this was an ascending or descending run
 *   <li>Ear indicates which ear was being tested
 *   <li>Timestamp enables temporal analysis and system auditing
 * </ul>
 *
 * @param frequency the stimulus frequency in Hz
 * @param dbLevel the stimulus level in clinical dB attenuation (0-120)
 * @param responseType the patient's response (HEARD, NOT_HEARD, or TIMEOUT)
 * @param phase the testing phase at the time of response (ASCENDING or DESCENDING)
 * @param timestamp the time when the response was recorded
 * @param ear the ear being tested (LEFT or RIGHT)
 */
public record ResponseRecord(
        int frequency,
        int dbLevel,
        ResponseType responseType,
        TestPhase phase,
        Instant timestamp,
        Ear ear) {
}
