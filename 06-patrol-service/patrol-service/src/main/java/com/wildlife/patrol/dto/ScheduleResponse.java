package com.wildlife.patrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.entity.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResponse(
        Long id,
        String code,
        Long teamId,
        String teamName,
        Long routeId,
        String routeName,
        Long areaId,
        Long zoneId,
        String zoneName,
        LocalDate patrolDate,
        Shift shift,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        PatrolStatus status,
        Long vehicleId,
        List<Long> allocationIds,
        String objective,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<String> warnings) {
}
