package com.wildlife.programme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProgressUpdateRequest(
        Long targetId,
        @NotNull @PastOrPresent LocalDate updateDate,
        Double newValue,
        @NotBlank @Size(max = 2000) String note) {
}
