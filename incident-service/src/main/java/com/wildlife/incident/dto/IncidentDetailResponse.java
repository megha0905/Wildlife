package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;

import java.time.LocalDateTime;
import java.util.List;

/** IncidentResponse fields plus history (CONTRACTS.md 7.6). */
public record IncidentDetailResponse(
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
        List<String> warnings,
        List<StatusHistoryResponse> history) {

    public static IncidentDetailResponse of(IncidentResponse r, List<StatusHistoryResponse> history) {
        return new IncidentDetailResponse(r.id(), r.incidentCode(), r.title(), r.incidentType(), r.severity(),
                r.status(), r.description(), r.areaId(), r.zoneId(), r.zoneName(), r.latitude(), r.longitude(),
                r.locationDescription(), r.occurredAt(), r.reportedAt(), r.reportedByUserId(), r.reportedByName(),
                r.verifiedAt(), r.resolvedAt(), r.closedAt(), r.resolutionNotes(), r.animalId(), r.speciesId(),
                r.sourcePatrolLogId(), r.assignedPersonnel(), r.warnings(), history);
    }
}
