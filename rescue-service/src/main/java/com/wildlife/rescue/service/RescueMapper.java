package com.wildlife.rescue.service;

import com.wildlife.rescue.dto.RehabProgressResponse;
import com.wildlife.rescue.dto.ReleaseInfo;
import com.wildlife.rescue.dto.RescueHistoryResponse;
import com.wildlife.rescue.dto.RescueResponse;
import com.wildlife.rescue.dto.TransferInfo;
import com.wildlife.rescue.dto.TreatmentResponse;
import com.wildlife.rescue.entity.RehabProgress;
import com.wildlife.rescue.entity.RescueCase;
import com.wildlife.rescue.entity.RescueStatusHistory;
import com.wildlife.rescue.entity.Treatment;

import java.util.List;

final class RescueMapper {

    private RescueMapper() {
    }

    static RescueResponse toRescue(RescueCase r, List<String> warnings) {
        TransferInfo transfer = r.getTransferDestination() == null ? null
                : new TransferInfo(r.getTransferDestination(), r.getTransferReason(), r.getTransferDate());
        ReleaseInfo release = r.getReleaseDate() == null ? null
                : new ReleaseInfo(r.getReleaseDate(), r.getReleaseAreaId(), r.getReleaseZoneId(),
                r.getReleaseLatitude(), r.getReleaseLongitude(), r.getReleaseNotes());
        return new RescueResponse(r.getId(), r.getCaseNumber(), r.getAnimalId(), r.getAnimalTagId(), r.getSpeciesId(),
                r.getSpeciesName(), r.getAnimalDescription(), r.getCause(), r.getIntakeCondition(), r.getStatus(),
                r.getRescuedAt(), r.getLatitude(), r.getLongitude(), r.getAreaId(), r.getZoneId(), r.getIncidentId(),
                r.getFacilityName(), r.getRegisteredByName(), r.getNotes(), transfer, release, r.getCreatedAt(),
                r.getUpdatedAt(), warnings == null ? List.of() : warnings);
    }

    static TreatmentResponse toTreatment(Treatment t) {
        return new TreatmentResponse(t.getId(), t.getRescue().getId(), t.getTreatmentDate(), t.getDiagnosis(),
                t.getProcedure(), t.getMedications(), t.getVetStaffId(), t.getVetName(), t.getNotes(), t.getCreatedAt());
    }

    static RehabProgressResponse toRehab(RehabProgress p) {
        return new RehabProgressResponse(p.getId(), p.getRescue().getId(), p.getAssessedOn(), p.getEnclosure(),
                p.getBehaviourNotes(), p.getWeightKg(), p.isFitForRelease(), p.getAssessedBy(), p.getCreatedAt());
    }

    static RescueHistoryResponse toHistory(RescueStatusHistory h) {
        return new RescueHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedByUserId(),
                h.getChangedByName(), h.getChangedAt(), h.getNote());
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
