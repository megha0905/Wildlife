package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;

import java.time.LocalDateTime;
import java.util.List;

/** One row of the monthly zone review (CONTRACTS.md 7.6). */
public record MonthlyIncidentItem(
        Long id,
        String incidentCode,
        String title,
        IncidentType incidentType,
        Severity severity,
        IncidentStatus status,
        LocalDateTime occurredAt,
        Double latitude,
        Double longitude,
        String locationDescription,
        List<AssignedPersonnel> assignedPersonnel) {
}
