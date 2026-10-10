package com.wildlife.patrol.client;

import java.time.LocalDateTime;

/** Body for incident-service POST /api/incidents when escalating a log (CONTRACTS.md 9.5). */
public record IncidentCreate(String title, String incidentType, String severity, String description,
                             Long areaId, Long zoneId, Double latitude, Double longitude,
                             String locationDescription, LocalDateTime occurredAt, Long sourcePatrolLogId) {
}
