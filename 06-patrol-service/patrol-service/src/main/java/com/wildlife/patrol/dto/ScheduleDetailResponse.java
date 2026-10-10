package com.wildlife.patrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.entity.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** ScheduleResponse fields plus team, route and logs (CONTRACTS.md 7.5). */
public record ScheduleDetailResponse(
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
        List<String> warnings,
        TeamResponse team,
        RouteResponse route,
        List<LogResponse> logs) {

    public static ScheduleDetailResponse of(ScheduleResponse s, TeamResponse team, RouteResponse route,
                                            List<LogResponse> logs) {
        return new ScheduleDetailResponse(s.id(), s.code(), s.teamId(), s.teamName(), s.routeId(), s.routeName(),
                s.areaId(), s.zoneId(), s.zoneName(), s.patrolDate(), s.shift(), s.startTime(), s.endTime(),
                s.status(), s.vehicleId(), s.allocationIds(), s.objective(), s.startedAt(), s.completedAt(),
                s.warnings(), team, route, logs);
    }
}
