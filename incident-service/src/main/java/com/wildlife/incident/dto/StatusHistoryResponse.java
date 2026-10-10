package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentStatus;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
        Long id,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        Long changedByUserId,
        String changedByName,
        LocalDateTime changedAt,
        String note) {
}
