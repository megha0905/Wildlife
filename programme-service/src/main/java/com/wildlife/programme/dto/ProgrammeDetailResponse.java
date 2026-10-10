package com.wildlife.programme.dto;

import com.wildlife.programme.entity.ProgrammeStatus;
import com.wildlife.programme.entity.ProgrammeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** ProgrammeResponse fields plus activities, targets and the latest 10 progress updates (CONTRACTS.md 7.8). */
public record ProgrammeDetailResponse(
        Long id,
        String code,
        String name,
        ProgrammeType programmeType,
        ProgrammeStatus status,
        String objective,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budgetInr,
        Long leadStaffId,
        String leadStaffName,
        List<Long> areaIds,
        List<Long> speciesIds,
        double progressPercent,
        long activityCount,
        long activitiesDone,
        long targetCount,
        List<String> warnings,
        List<ActivityResponse> activities,
        List<TargetResponse> targets,
        List<ProgressUpdateResponse> recentProgress) {

    public static ProgrammeDetailResponse of(ProgrammeResponse p, List<ActivityResponse> activities,
                                             List<TargetResponse> targets, List<ProgressUpdateResponse> recent) {
        return new ProgrammeDetailResponse(p.id(), p.code(), p.name(), p.programmeType(), p.status(), p.objective(),
                p.description(), p.startDate(), p.endDate(), p.budgetInr(), p.leadStaffId(), p.leadStaffName(),
                p.areaIds(), p.speciesIds(), p.progressPercent(), p.activityCount(), p.activitiesDone(),
                p.targetCount(), p.warnings(), activities, targets, recent);
    }
}
