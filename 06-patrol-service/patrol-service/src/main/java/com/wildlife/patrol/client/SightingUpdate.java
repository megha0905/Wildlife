package com.wildlife.patrol.client;

import java.time.LocalDateTime;

/** Body for wildlife-service PATCH /api/animals/{id}/sighting (CONTRACTS.md 9.3). */
public record SightingUpdate(LocalDateTime seenAt, Double latitude, Double longitude, Long zoneId, Long areaId,
                             String healthStatus, String sourceReference, String note) {
}
