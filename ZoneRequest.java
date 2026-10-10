package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.ZoneType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ZoneRequest(
        @NotNull Long areaId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(min = 2, max = 20) String code,
        @NotNull ZoneType zoneType,
        @Positive Double areaSqKm,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double centerLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double centerLongitude,
        @Size(max = 2000) String description) {
}
