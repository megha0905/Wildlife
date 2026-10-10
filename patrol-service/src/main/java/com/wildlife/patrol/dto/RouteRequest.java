package com.wildlife.patrol.dto;

import com.wildlife.patrol.entity.RouteDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RouteRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull Long areaId,
        @NotNull Long zoneId,
        @NotNull @Positive Double distanceKm,
        @Positive Double estimatedHours,
        @NotNull RouteDifficulty difficulty,
        List<@Valid WaypointDto> waypoints,
        @Size(max = 1000) String description) {
}
