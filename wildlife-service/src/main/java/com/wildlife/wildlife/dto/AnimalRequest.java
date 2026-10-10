package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.HealthStatus;
import com.wildlife.wildlife.entity.Sex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AnimalRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{3,30}$", message = "must be 3-30 letters, digits or '-'")
        String tagId,
        @Size(max = 80) String name,
        @NotNull Long speciesId,
        @NotNull Sex sex,
        @Min(1900) Integer birthYear,
        Boolean birthYearEstimated,
        @NotNull HealthStatus healthStatus,
        @NotNull AnimalStatus status,
        @Size(max = 1000) String identificationMarks,
        Long areaId,
        Long zoneId,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double lastSeenLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double lastSeenLongitude,
        LocalDateTime lastSeenAt,
        @Size(max = 2000) String notes) {
}
