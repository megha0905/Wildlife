package com.wildlife.patrol.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AllocationDto(Long id, String resourceKind, Long resourceId, String resourceLabel, String purpose,
                            Long referenceId, LocalDateTime startAt, LocalDateTime endAt, String status) {
}
