package com.wildlife.programme.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.8 / 9.6). */
public record ProgrammeSummary(
        Map<String, Long> byStatus,
        Map<String, Long> byType,
        long activeProgrammes,
        double averageProgressPercent,
        long overdueActivities,
        BigDecimal totalBudgetInrActive) {
}
