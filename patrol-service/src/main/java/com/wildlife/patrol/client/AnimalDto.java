package com.wildlife.patrol.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Our copy of wildlife-service's AnimalDto (CONTRACTS.md 9.3). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalDto(Long id, String tagId, String name, Long speciesId, String speciesCommonName,
                        String healthStatus, String status, Long areaId, Long zoneId) {
}
