package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentResponse(
        Long id,
        String incidentCode,
        String title,
        IncidentType incidentType,
        Severity severity,
        IncidentStatus status,
        String description,
        Long areaId,
        Long zoneId,
        String zoneName,
        Double latitude,
        Double longitude,
        String locationDescription,
        LocalDateTime occurredAt,
        LocalDateTime reportedAt,
        Long reportedByUserId,
        String reportedByName,
        LocalDateTime verifiedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        String resolutionNotes,
        Long animalId,
        Long speciesId,
        Long sourcePatrolLogId,
        List<AssignedPersonnel> assignedPersonnel,
        List<String> warnings) {
}
