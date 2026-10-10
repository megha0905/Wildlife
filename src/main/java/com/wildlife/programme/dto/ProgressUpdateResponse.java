package com.wildlife.programme.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProgressUpdateResponse(
        Long id,
        Long programmeId,
        Long targetId,
        String targetMetricName,
        LocalDate updateDate,
        Double previousValue,
        Double newValue,
        String note,
        Long reportedByUserId,
        String reportedByName,
        LocalDateTime createdAt) {
}
