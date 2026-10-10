package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.HealthStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sent by rescue-service on case transitions (CONTRACTS.md 9.4) and by the UI. */
public record AnimalStatusUpdate(
        @NotNull AnimalStatus status,
        HealthStatus healthStatus,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Long areaId,
        Long zoneId,
        @Size(max = 60) String sourceReference,
        @Size(max = 2000) String note) {
}
