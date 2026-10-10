package com.wildlife.rescue.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReleaseInfo(
        @NotNull LocalDate releaseDate,
        @NotNull Long releaseAreaId,
        @NotNull Long releaseZoneId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 1000) String notes) {
}
