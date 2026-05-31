package com.audiometer.domain;

/**
 * Immutable record representing the detected hearing threshold at a specific frequency and ear.
 *
 * <p>A ThresholdResult captures the outcome of the threshold detection process for one combination
 * of frequency and ear. It represents the quietest level at which a patient reliably responds to
 * a tone stimulus, which is the fundamental measurement in clinical audiometry.
 *
 * <p><strong>Immutability:</strong> ThresholdResult is a Java record and is fully immutable.
 *
 * <p><strong>Clinical Significance:</strong>
 * <ul>
 *   <li>Threshold values typically range from -10 dB (better hearing) to 120 dB (profound loss)
 *   <li>A lower dB value indicates better hearing sensitivity
 *   <li>Normal hearing is typically defined as ≤20 dB
 *   <li>The complete audiogram is composed of threshold measurements at multiple frequencies and
 *       both ears
 * </ul>
 *
 * @param frequency the test frequency in Hz
 * @param ear the ear for which the threshold was measured
 * @param thresholdDb the hearing threshold in clinical dB attenuation
 */
public record ThresholdResult(int frequency, Ear ear, int thresholdDb) {
}
