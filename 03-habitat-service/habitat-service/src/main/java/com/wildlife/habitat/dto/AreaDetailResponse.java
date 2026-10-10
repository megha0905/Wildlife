package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.AreaType;

import java.util.List;

/** AreaResponse plus its zones (CONTRACTS.md 7.2). */
public record AreaDetailResponse(
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
        long zoneCount,
        List<ZoneResponse> zones) {
}
