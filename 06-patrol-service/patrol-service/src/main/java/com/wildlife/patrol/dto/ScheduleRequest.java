package com.wildlife.patrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wildlife.patrol.entity.Shift;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull Long teamId,
        @NotNull Long routeId,
        @NotNull LocalDate patrolDate,
        @NotNull Shift shift,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Long vehicleId,
        @Size(max = 500) String objective) {
}
