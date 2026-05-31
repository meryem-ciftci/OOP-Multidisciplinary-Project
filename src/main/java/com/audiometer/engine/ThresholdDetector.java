package com.audiometer.engine;

import com.audiometer.domain.Ear;
import com.audiometer.domain.ResponseRecord;
import com.audiometer.domain.ResponseType;
import com.audiometer.domain.TestPhase;
import com.audiometer.domain.ThresholdResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implements clinical threshold detection criteria for pure-tone audiometry.
 *
 * <p>This class encapsulates the decision logic for determining when a hearing threshold has been
 * established at a given frequency. The threshold detection uses response history with
 * clinically-validated criteria to ensure reliable and reproducible threshold identification.
 *
 * <p><strong>Threshold Criteria:</strong>
 * <ul>
 *   <li>Based on ascending-run responses only (clinically standard for Hughson-Westlake)
 *   <li>Requires at least 2 HEARD responses at the same dB level
 *   <li>Requires at least 3 total responses at that dB level (indicating stable performance)
 *   <li>Returns the lowest level meeting these criteria (conservative, clinically safer)
 *   <li>Ignores TIMEOUT responses (treats as NOT_HEARD for threshold determination)
 * </ul>
 *
 * <p><strong>Immutability:</strong> This class is stateless and immutable. The
 * {@link #detectThreshold(List, Ear, int)} method is a pure function with no side effects.
 *
 * <p><strong>Clinical Significance:</strong> Proper threshold detection is critical for accurate
 * hearing assessment. The criteria implemented here balance sensitivity (detecting true thresholds)
 * with specificity (avoiding false-positive thresholds due to attention lapses or measurement
 * noise).
 *
 * @see HughsonWestlakeEngine for integration into the complete test workflow
 */
public final class ThresholdDetector {

    /**
     * Detects whether a hearing threshold has been established at the given frequency and ear based
     * on accumulated response history.
     *
     * <p>This method applies clinical threshold criteria to the ascending-run responses:
     *
     * <ol>
     *   <li>Filters responses to only ascending-run trials for the specified ear and frequency
     *   <li>Identifies response levels where the patient reported HEARD at least 2 times
     *   <li>Further filters to levels with at least 3 total test attempts (indicating convergence)
     *   <li>Returns the lowest qualifying level as the threshold (conservative approach)
     * </ol>
     *
     * <p>TIMEOUT responses are treated as NOT_HEARD, which is the conservative clinical choice.
     *
     * @param history a list of all response records from the test session
     * @param ear the ear for which to detect the threshold
     * @param frequency the test frequency in Hz
     * @return an {@link Optional} containing the detected {@link ThresholdResult} if threshold
     *     criteria are met, or empty if threshold has not yet been established
     */
    public Optional<ThresholdResult> detectThreshold(List<ResponseRecord> history, Ear ear, int frequency) {
        List<ResponseRecord> ascendingResponses = history.stream()
                .filter(record -> record.ear() == ear)
                .filter(record -> record.frequency() == frequency)
                .filter(record -> record.phase() == TestPhase.ASCENDING)
                .toList();

        // Count HEARD responses at each level
        Map<Integer, Long> heardAtLevel = ascendingResponses.stream()
                .filter(record -> toClinicalResponse(record.responseType()) == ResponseType.HEARD)
                .collect(Collectors.groupingBy(ResponseRecord::dbLevel, Collectors.counting()));

        // Find the lowest level meeting both criteria:
        // 1. At least 2 HEARD responses at this level
        // 2. At least 3 total responses at this level
        return heardAtLevel.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(Map.Entry::getKey)
                .sorted()
                .filter(level -> ascendingResponses.stream().filter(record -> record.dbLevel() == level).count() >= 3)
                .map(level -> new ThresholdResult(frequency, ear, level))
                .findFirst();
    }

    /**
     * Maps response types to their clinical interpretation.
     *
     * <p>TIMEOUT is conservatively treated as NOT_HEARD (patient did not respond).
     *
     * @param responseType the raw response type
     * @return the clinical interpretation: HEARD or NOT_HEARD
     */
    private ResponseType toClinicalResponse(ResponseType responseType) {
        return responseType == ResponseType.TIMEOUT ? ResponseType.NOT_HEARD : responseType;
    }
}
