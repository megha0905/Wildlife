package com.wildlife.programme.dto;

import com.wildlife.programme.entity.ProgrammeStatus;
import com.wildlife.programme.entity.ProgrammeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProgrammeResponse(
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
        List<String> warnings) {
}
