package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.HealthRecordType;
import com.wildlife.wildlife.entity.HealthStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthRecordResponse(
        Long id,
        Long animalId,
        LocalDate recordDate,
        HealthRecordType recordType,
        HealthStatus healthStatus,
        Double weightKg,
        String diagnosis,
        String treatment,
        String vetName,
        Long recordedByUserId,
        String recordedByName,
        String notes,
        LocalDateTime createdAt) {
}
