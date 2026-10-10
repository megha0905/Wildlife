package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.AreaType;

public record AreaResponse(
        Long id,
        String name,
        String code,
        AreaType areaType,
        String state,
        String district,
        Double totalAreaSqKm,
        Integer establishedYear,
        Double centerLatitude,
        Double centerLongitude,
        String description,
        long zoneCount) {
}
