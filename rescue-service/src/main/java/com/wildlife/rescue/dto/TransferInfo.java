package com.wildlife.rescue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TransferInfo(
        @NotBlank @Size(max = 150) String destinationFacility,
        @NotBlank @Size(max = 500) String reason,
        @NotNull LocalDate transferDate) {
}
