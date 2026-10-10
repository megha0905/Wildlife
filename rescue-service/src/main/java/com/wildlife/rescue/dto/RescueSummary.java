package com.wildlife.rescue.dto;

import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.7 / 9.6). */
public record RescueSummary(
        long activeCases,
        Map<String, Long> byStatus,
        Map<String, Long> byCause,
        long releasedThisYear,
        long transferredThisYear,
        long admittedThisMonth) {
}
