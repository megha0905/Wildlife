package com.wildlife.rescue.dto;

import com.wildlife.rescue.entity.HealthStatus;
import com.wildlife.rescue.entity.RescueCause;
import com.wildlife.rescue.entity.RescueStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RescueResponse(
        Long id,
        String caseNumber,
        Long animalId,
        String animalTagId,
        Long speciesId,
        String speciesName,
        String animalDescription,
        RescueCause cause,
        HealthStatus intakeCondition,
        RescueStatus status,
        LocalDateTime rescuedAt,
        Double latitude,
        Double longitude,
        Long areaId,
        Long zoneId,
        Long incidentId,
        String facilityName,
        String registeredByName,
        String notes,
        TransferInfo transfer,
        ReleaseInfo release,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> warnings) {
}
