package com.wildlife.habitat.dto;

import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.2 / 9.6). Maps always contain every enum value. */
public record HabitatSummary(
        long totalAreas,
        long totalZones,
        double totalAreaSqKm,
        Map<String, Long> habitatsByCondition,
        Map<String, Long> waterByStatus,
        long waterSourcesNeedingAttention) {
}
