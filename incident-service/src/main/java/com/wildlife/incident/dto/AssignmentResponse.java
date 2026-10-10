package com.wildlife.incident.dto;

import com.wildlife.incident.entity.AssignmentRole;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponse(
        Long id,
        Long incidentId,
        Long staffId,
        String staffName,
        String designation,
        AssignmentRole assignmentRole,
        LocalDateTime assignedAt,
        String assignedByName,
        boolean active,
        String note,
        Long allocationId,
        List<String> warnings) {
}
