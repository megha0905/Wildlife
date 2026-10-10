package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.ConservationStatus;
import com.wildlife.wildlife.entity.HealthStatus;
import com.wildlife.wildlife.entity.Sex;

import java.time.LocalDateTime;
import java.util.List;

/** Also read by patrol and rescue services as AnimalDto (CONTRACTS.md 9.3). Do not rename fields. */
public record AnimalResponse(
        Long id,
        String tagId,
        String name,
        Long speciesId,
        String speciesCommonName,
        String speciesScientificName,
        ConservationStatus conservationStatus,
        Sex sex,
        Integer birthYear,
        boolean birthYearEstimated,
        Integer ageYears,
        HealthStatus healthStatus,
        AnimalStatus status,
        String identificationMarks,
        Long areaId,
        Long zoneId,
        String zoneName,
        Double lastSeenLatitude,
        Double lastSeenLongitude,
        LocalDateTime lastSeenAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> warnings) {
}
