package com.wildlife.programme.service;

import java.util.Collection;

/**
 * Progress maths (CONTRACTS.md 7.8). Works for increasing targets (e.g. camera traps 10 -> 50)
 * and decreasing ones (e.g. snares found 40 -> 0).
 */
public final class ProgressCalculator {

    private ProgressCalculator() {
    }

    /** (current - baseline) / (target - baseline) x 100, clamped to 0..100, one decimal. */
    public static double targetPercent(double baseline, double target, double current) {
        if (target == baseline) {
            return current == target ? 100.0 : 0.0;
        }
        double ratio = (current - baseline) / (target - baseline);
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        return Math.round(clamped * 1000.0) / 10.0;
    }

    /** Average of the targets' percentages; 0 when there are no targets. */
    public static double average(Collection<Double> percents) {
        if (percents.isEmpty()) return 0.0;
        double avg = percents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return Math.round(avg * 10.0) / 10.0;
    }
}
