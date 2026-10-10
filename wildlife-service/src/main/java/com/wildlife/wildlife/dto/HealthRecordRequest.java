package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.HealthRecordType;
import com.wildlife.wildlife.entity.HealthStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HealthRecordRequest(
        @NotNull @PastOrPresent LocalDate recordDate,
        @NotNull HealthRecordType recordType,
        @NotNull HealthStatus healthStatus,
        @Positive Double weightKg,
        @Size(max = 500) String diagnosis,
        @Size(max = 1000) String treatment,
        @Size(max = 120) String vetName,
        @Size(max = 2000) String notes) {
}
