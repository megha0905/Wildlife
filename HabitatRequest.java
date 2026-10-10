package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.HabitatCondition;
import com.wildlife.habitat.entity.HabitatType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HabitatRequest(
        @NotNull Long zoneId,
        @NotBlank @Size(max = 150) String name,
        @NotNull HabitatType habitatType,
        @NotNull HabitatCondition condition,
        @Positive Double areaSqKm,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 300) String dominantVegetation,
        @PastOrPresent LocalDate lastSurveyDate,
        @Size(max = 2000) String notes) {
}
