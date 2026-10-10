package com.wildlife.rescue.dto;

import com.wildlife.rescue.entity.HealthStatus;
import com.wildlife.rescue.entity.RescueCause;
import com.wildlife.rescue.entity.RescueStatus;

import java.time.LocalDateTime;
import java.util.List;

/** RescueResponse fields plus treatments, rehab progress and history (CONTRACTS.md 7.7). */
public record RescueDetailResponse(
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
        List<String> warnings,
        List<TreatmentResponse> treatments,
        List<RehabProgressResponse> rehabProgress,
        List<RescueHistoryResponse> history) {

    public static RescueDetailResponse of(RescueResponse r, List<TreatmentResponse> treatments,
                                          List<RehabProgressResponse> rehab, List<RescueHistoryResponse> history) {
        return new RescueDetailResponse(r.id(), r.caseNumber(), r.animalId(), r.animalTagId(), r.speciesId(),
                r.speciesName(), r.animalDescription(), r.cause(), r.intakeCondition(), r.status(), r.rescuedAt(),
                r.latitude(), r.longitude(), r.areaId(), r.zoneId(), r.incidentId(), r.facilityName(),
                r.registeredByName(), r.notes(), r.transfer(), r.release(), r.createdAt(), r.updatedAt(),
                r.warnings(), treatments, rehab, history);
    }
}
