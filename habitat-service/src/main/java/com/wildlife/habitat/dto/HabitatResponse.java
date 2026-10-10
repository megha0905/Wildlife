package com.wildlife.habitat.dto;

import com.wildlife.habitat.entity.HabitatCondition;
import com.wildlife.habitat.entity.HabitatType;

import java.time.LocalDate;

public record HabitatResponse(
        Long id,
        Long zoneId,
        String zoneName,
        Long areaId,
        String name,
        HabitatType habitatType,
        HabitatCondition condition,
        Double areaSqKm,
        Double latitude,
        Double longitude,
        String dominantVegetation,
        LocalDate lastSurveyDate,
        String notes) {
}
