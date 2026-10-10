package com.wildlife.incident.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AllocationDto(Long id, String resourceKind, Long resourceId, String status) {
}
