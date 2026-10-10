package com.wildlife.rescue.client;

/** Body for wildlife-service PATCH /api/animals/{id}/status (CONTRACTS.md 9.3 / 9.4). */
public record AnimalStatusUpdate(String status, String healthStatus, Double latitude, Double longitude,
                                 Long areaId, Long zoneId, String sourceReference, String note) {
}
