package com.wildlife.programme.dto;

import com.wildlife.programme.entity.ActivityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ActivityRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull LocalDate plannedStart,
        @NotNull LocalDate plannedEnd,
        @NotNull ActivityStatus status,
        Long assignedStaffId,
        Long areaId) {
}
