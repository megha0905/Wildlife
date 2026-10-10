package com.wildlife.incident.service;

import com.wildlife.incident.dto.AssignedPersonnel;
import com.wildlife.incident.dto.AssignmentResponse;
import com.wildlife.incident.dto.IncidentResponse;
import com.wildlife.incident.dto.MonthlyIncidentItem;
import com.wildlife.incident.dto.StatusHistoryResponse;
import com.wildlife.incident.entity.Incident;
import com.wildlife.incident.entity.IncidentAssignment;
import com.wildlife.incident.entity.IncidentStatusHistory;

import java.util.List;

final class IncidentMapper {

    private IncidentMapper() {
    }

    static AssignedPersonnel toPersonnel(IncidentAssignment a) {
        return new AssignedPersonnel(a.getId(), a.getStaffId(), a.getStaffName(), a.getDesignation(),
                a.getAssignmentRole(), a.getAssignedAt());
    }

    static IncidentResponse toIncident(Incident i, List<AssignedPersonnel> personnel, List<String> warnings) {
        return new IncidentResponse(i.getId(), i.getIncidentCode(), i.getTitle(), i.getIncidentType(),
                i.getSeverity(), i.getStatus(), i.getDescription(), i.getAreaId(), i.getZoneId(), i.getZoneName(),
                i.getLatitude(), i.getLongitude(), i.getLocationDescription(), i.getOccurredAt(), i.getReportedAt(),
                i.getReportedByUserId(), i.getReportedByName(), i.getVerifiedAt(), i.getResolvedAt(),
                i.getClosedAt(), i.getResolutionNotes(), i.getAnimalId(), i.getSpeciesId(), i.getSourcePatrolLogId(),
                personnel == null ? List.of() : personnel, warnings == null ? List.of() : warnings);
    }

    static MonthlyIncidentItem toMonthlyItem(Incident i, List<AssignedPersonnel> personnel) {
        return new MonthlyIncidentItem(i.getId(), i.getIncidentCode(), i.getTitle(), i.getIncidentType(),
                i.getSeverity(), i.getStatus(), i.getOccurredAt(), i.getLatitude(), i.getLongitude(),
                i.getLocationDescription(), personnel);
    }

    static StatusHistoryResponse toHistory(IncidentStatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedByUserId(),
                h.getChangedByName(), h.getChangedAt(), h.getNote());
    }

    static AssignmentResponse toAssignment(IncidentAssignment a, List<String> warnings) {
        return new AssignmentResponse(a.getId(), a.getIncident().getId(), a.getStaffId(), a.getStaffName(),
                a.getDesignation(), a.getAssignmentRole(), a.getAssignedAt(), a.getAssignedByName(), a.isActive(),
                a.getNote(), a.getAllocationId(), warnings == null ? List.of() : warnings);
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
