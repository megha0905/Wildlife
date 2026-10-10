package com.wildlife.rescue.dto;

import com.wildlife.rescue.entity.HealthStatus;
import com.wildlife.rescue.entity.RescueCause;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RescueRequest(
        Long animalId,
        Long speciesId,
        @NotBlank @Size(max = 120) String speciesName,
        @Size(max = 500) String animalDescription,
        @NotNull RescueCause cause,
        @NotNull HealthStatus intakeCondition,
        @NotNull LocalDateTime rescuedAt,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Long areaId,
        Long zoneId,
        Long incidentId,
        @NotBlank @Size(max = 150) String facilityName,
        @Size(max = 2000) String notes) {
}
