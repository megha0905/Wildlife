package com.wildlife.programme;

import com.wildlife.programme.service.ProgressCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CONTRACTS.md 7.8 progress rules. */
class ProgressCalculatorTest {

    @Test
    void increasingTarget() {
        // camera traps 10 -> 50, now 30 => half way
        assertThat(ProgressCalculator.targetPercent(10, 50, 30)).isEqualTo(50.0);
    }

    @Test
    void decreasingTargetLikeSnaresFound() {
        // snares 40 -> 0, now 12 => 70 % of the way
        assertThat(ProgressCalculator.targetPercent(40, 0, 12)).isEqualTo(70.0);
    }

    @Test
    void clampedBetweenZeroAndHundred() {
        assertThat(ProgressCalculator.targetPercent(10, 50, 70)).isEqualTo(100.0);
        assertThat(ProgressCalculator.targetPercent(10, 50, 5)).isEqualTo(0.0);
        assertThat(ProgressCalculator.targetPercent(40, 0, 55)).isEqualTo(0.0);
    }

    @Test
    void targetEqualToBaseline() {
        assertThat(ProgressCalculator.targetPercent(5, 5, 5)).isEqualTo(100.0);
        assertThat(ProgressCalculator.targetPercent(5, 5, 4)).isEqualTo(0.0);
    }

    @Test
    void roundedToOneDecimal() {
        assertThat(ProgressCalculator.targetPercent(0, 3, 1)).isEqualTo(33.3);
    }

    @Test
    void programmeAverage() {
        assertThat(ProgressCalculator.average(List.of(70.0, 50.0))).isEqualTo(60.0);
        assertThat(ProgressCalculator.average(List.of())).isEqualTo(0.0);
    }
}
