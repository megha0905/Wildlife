package com.wildlife.reporting.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GET /api/reports/dashboard (CONTRACTS.md 7.9). Each section is the other service's /summary JSON,
 * or null when that service could not be reached (servicesOnline says which).
 */
public record DashboardResponse(
        LocalDateTime generatedAt,
        JsonNode habitat,
        JsonNode wildlife,
        JsonNode resources,
        JsonNode patrols,
        JsonNode incidents,
        JsonNode rescues,
        JsonNode programmes,
        Map<String, Boolean> servicesOnline,
        List<String> warnings) {
}
