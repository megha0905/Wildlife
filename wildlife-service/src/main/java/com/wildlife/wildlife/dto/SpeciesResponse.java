package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.ConservationStatus;
import com.wildlife.wildlife.entity.SpeciesCategory;

public record SpeciesResponse(
        Long id,
        String commonName,
        String scientificName,
        SpeciesCategory category,
        ConservationStatus conservationStatus,
        String protectionSchedule,
        String description,
        long animalCount) {
}
