package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.WaterSourceType;
import com.wildlife.habitat.entity.WaterStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record WaterResourceRequest(
        @NotNull Long zoneId,
        @NotBlank @Size(max = 150) String name,
        @NotNull WaterSourceType sourceType,
        @NotNull WaterStatus status,
        @NotNull Boolean perennial,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Positive Double capacityCubicM,
        LocalDateTime lastInspectedAt,
        @Size(max = 2000) String notes) {
}
