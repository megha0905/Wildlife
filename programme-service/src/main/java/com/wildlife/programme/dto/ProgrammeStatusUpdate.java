package com.wildlife.programme.dto;

import com.wildlife.programme.entity.ProgrammeStatus;
import jakarta.validation.constraints.NotNull;

public record ProgrammeStatusUpdate(@NotNull ProgrammeStatus status) {
}
