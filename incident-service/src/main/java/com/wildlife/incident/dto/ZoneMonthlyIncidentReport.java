package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GET /api/incidents/zone/{zoneId}/current-month?status= (CONTRACTS.md 7.6).
 * statusSummary always counts all five statuses for the month, ignoring the status filter;
 * incidents contains only the filtered status when one is given.
 */
public record ZoneMonthlyIncidentReport(
        Long zoneId,
        String zoneName,
        Long areaId,
        String areaName,
        String month,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        IncidentStatus statusFilter,
        long totalIncidents,
        Map<String, Long> statusSummary,
        List<MonthlyIncidentItem> incidents,
        List<String> warnings) {
}
