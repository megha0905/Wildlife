package com.wildlife.resource.dto;

import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.4 / 9.6). Maps always contain every enum value. */
public record ResourceSummary(
        long totalStaff,
        Map<String, Long> staffByStatus,
        Map<String, Long> staffByDesignation,
        Map<String, Long> vehiclesByStatus,
        Map<String, Long> equipmentByStatus,
        long activeAllocations,
        long allocationsToday) {
}
