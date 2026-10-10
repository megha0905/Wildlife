package com.wildlife.incident.dto;

import com.wildlife.incident.entity.AssignmentRole;

import java.time.LocalDateTime;

public record AssignedPersonnel(
        Long assignmentId,
        Long staffId,
        String staffName,
        String designation,
        AssignmentRole assignmentRole,
        LocalDateTime assignedAt) {
}
