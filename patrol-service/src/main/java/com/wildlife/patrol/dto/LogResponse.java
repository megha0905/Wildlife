package com.wildlife.patrol.dto;

import com.wildlife.patrol.entity.ObservationType;
import com.wildlife.patrol.entity.Severity;

import java.time.LocalDateTime;
import java.util.List;

public record LogResponse(
        Long id,
        Long scheduleId,
        LocalDateTime loggedAt,
        ObservationType observationType,
        Double latitude,
        Double longitude,
        String description,
        String animalTagId,
        Severity severityHint,
        Long reportedByUserId,
        String reportedByName,
        Long incidentId,
        LocalDateTime createdAt,
        List<String> warnings) {
}
