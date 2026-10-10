package com.wildlife.programme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TargetRequest(
        @NotBlank @Size(max = 150) String metricName,
        @NotBlank @Size(max = 30) String unit,
        @NotNull Double baselineValue,
        @NotNull Double targetValue,
        Double currentValue,
        LocalDate dueDate) {
}
