package com.wildlife.wildlife.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Our copy of habitat-service's zone (CONTRACTS.md 9.1). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ZoneDto(Long id, Long areaId, String areaName, String name, String code, String zoneType,
                      Double areaSqKm, Double centerLatitude, Double centerLongitude, String description) {
}
