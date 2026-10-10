package com.wildlife.rescue.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IncidentDto(Long id, String incidentCode, String title, String incidentType, String severity,
                          String status, Long areaId, Long zoneId) {
}
