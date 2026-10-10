package com.wildlife.rescue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RehabProgressRequest(
        @NotNull @PastOrPresent LocalDate assessedOn,
        @Size(max = 100) String enclosure,
        @NotBlank @Size(max = 1000) String behaviourNotes,
        @Positive Double weightKg,
        @NotNull Boolean fitForRelease,
        @NotBlank @Size(max = 120) String assessedBy) {
}
