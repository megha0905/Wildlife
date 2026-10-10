package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.WaterSourceType;
import com.wildlife.habitat.entity.WaterStatus;

import java.time.LocalDateTime;

public record WaterResourceResponse(
        Long id,
        Long zoneId,
        String zoneName,
        Long areaId,
        String name,
        WaterSourceType sourceType,
        WaterStatus status,
        boolean perennial,
        Double latitude,
        Double longitude,
        Double capacityCubicM,
        LocalDateTime lastInspectedAt,
        String notes) {
}
