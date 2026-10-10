package com.wildlife.wildlife.dto;

import com.wildlife.wildlife.entity.ConservationStatus;
import com.wildlife.wildlife.entity.SpeciesCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpeciesRequest(
        @NotBlank @Size(max = 120) String commonName,
        @NotBlank @Size(max = 160) String scientificName,
        @NotNull SpeciesCategory category,
        @NotNull ConservationStatus conservationStatus,
        @Size(max = 60) String protectionSchedule,
        @Size(max = 2000) String description) {
}
