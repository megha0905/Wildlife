package com.wildlife.patrol.dto;

import com.wildlife.patrol.entity.PatrolStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleStatusUpdate(@NotNull PatrolStatus status, @Size(max = 500) String note) {
}
