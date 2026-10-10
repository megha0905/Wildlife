package com.wildlife.patrol.dto;

import com.wildlife.patrol.entity.RouteDifficulty;

import java.util.List;

public record RouteResponse(
        Long id,
        String name,
        Long areaId,
        Long zoneId,
        String zoneName,
        Double distanceKm,
        Double estimatedHours,
        RouteDifficulty difficulty,
        List<WaypointDto> waypoints,
        String description,
        List<String> warnings) {
}
