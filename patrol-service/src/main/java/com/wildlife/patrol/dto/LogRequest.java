package com.wildlife.patrol.dto;

import com.wildlife.patrol.entity.ObservationType;
import com.wildlife.patrol.entity.Severity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record LogRequest(
        @NotNull LocalDateTime loggedAt,
        @NotNull ObservationType observationType,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank @Size(max = 2000) String description,
        @Size(max = 30) String animalTagId,
        Severity severityHint) {
}
