package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.HealthStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Sent by patrol-service when a patrol log mentions an animal tag. */
public record SightingUpdate(
        @NotNull LocalDateTime seenAt,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Long zoneId,
        Long areaId,
        HealthStatus healthStatus,
        @Size(max = 60) String sourceReference,
        @Size(max = 2000) String note) {
}
