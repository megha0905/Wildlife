package com.wildlife.patrol.dto;

import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.5 / 9.6). */
public record PatrolSummary(
        long schedulesToday,
        Map<String, Long> schedulesThisMonthByStatus,
        long logsThisMonth,
        long suspiciousActivityThisMonth,
        double distancePatrolledKmThisMonth) {
}
