package com.wildlife.rescue.dto;

import com.wildlife.rescue.entity.RescueStatus;

import java.time.LocalDateTime;

public record RescueHistoryResponse(
        Long id,
        RescueStatus fromStatus,
        RescueStatus toStatus,
        Long changedByUserId,
        String changedByName,
        LocalDateTime changedAt,
        String note) {
}
