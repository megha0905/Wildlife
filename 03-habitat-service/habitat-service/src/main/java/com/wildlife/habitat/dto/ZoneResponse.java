package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.ZoneType;

/** Also the cross-service ZoneDto other services read (CONTRACTS.md 9.1). Do not rename fields. */
public record ZoneResponse(
        Long id,
        Long areaId,
        String areaName,
        String name,
        String code,
        ZoneType zoneType,
        Double areaSqKm,
        Double centerLatitude,
        Double centerLongitude,
        String description) {
}
