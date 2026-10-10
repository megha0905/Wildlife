package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.AreaType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AreaRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(min = 2, max = 20) String code,
        @NotNull AreaType areaType,
        @NotBlank @Size(max = 80) String state,
        @Size(max = 80) String district,
        @NotNull @Positive Double totalAreaSqKm,
        @Min(1800) @Max(2100) Integer establishedYear,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double centerLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double centerLongitude,
        @Size(max = 2000) String description) {
}
