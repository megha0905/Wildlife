package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Also sent by patrol-service when a log is escalated (CONTRACTS.md 9.5). */
public record IncidentRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull IncidentType incidentType,
        @NotNull Severity severity,
        @NotBlank @Size(max = 4000) String description,
        @NotNull Long areaId,
        @NotNull Long zoneId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 300) String locationDescription,
        @NotNull LocalDateTime occurredAt,
        Long animalId,
        Long speciesId,
        Long sourcePatrolLogId) {
}
