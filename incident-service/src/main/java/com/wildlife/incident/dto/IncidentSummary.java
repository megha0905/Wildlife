package com.wildlife.incident.dto;

import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.6 / 9.6). Maps always contain every enum value. */
public record IncidentSummary(
        int year,
        int month,
        long totalThisMonth,
        long openIncidents,
        Map<String, Long> byStatus,
        Map<String, Long> byType,
        Map<String, Long> bySeverity,
        long criticalOpen) {
}
