package com.wildlife.programme.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TargetResponse(
        Long id,
        Long programmeId,
        String metricName,
        String unit,
        Double baselineValue,
        Double targetValue,
        Double currentValue,
        LocalDate dueDate,
        double progressPercent,
        LocalDateTime lastUpdatedAt) {
}
