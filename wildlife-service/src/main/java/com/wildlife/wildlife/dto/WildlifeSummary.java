package com.wildlife.wildlife.dto;

import java.util.Map;

/** Read by reporting-service (CONTRACTS.md 7.3 / 9.6). Maps always contain every enum value. */
public record WildlifeSummary(
        long totalSpecies,
        long totalAnimals,
        Map<String, Long> byHealthStatus,
        Map<String, Long> byStatus,
        Map<String, Long> byConservationStatus,
        long needingAttention,
        long notSeenFor30Days) {
}
