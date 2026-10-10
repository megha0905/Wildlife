package com.wildlife.rescue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TreatmentRequest(
        @NotNull @PastOrPresent LocalDate treatmentDate,
        @NotBlank @Size(max = 500) String diagnosis,
        @NotBlank @Size(max = 1000) String procedure,
        @Size(max = 500) String medications,
        Long vetStaffId,
        @NotBlank @Size(max = 120) String vetName,
        @Size(max = 1000) String notes) {
}
