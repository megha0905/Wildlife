package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.WaterStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record WaterStatusUpdate(
        @NotNull WaterStatus status,
        LocalDateTime inspectedAt,
        @Size(max = 2000) String notes) {
}
