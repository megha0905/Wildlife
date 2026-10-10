package com.wildlife.reporting.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** GET /api/reports/zones/{zoneId} (CONTRACTS.md 7.9). */
public record ZoneReport(
        JsonNode zone,
        Long animalsInZone,
        JsonNode incidentsThisMonth,
        JsonNode waterResources,
        List<String> warnings) {
}
