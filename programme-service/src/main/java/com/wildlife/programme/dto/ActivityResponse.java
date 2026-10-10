package com.wildlife.programme.dto;

import com.wildlife.programme.entity.ActivityStatus;

import java.time.LocalDate;

public record ActivityResponse(
        Long id,
        Long programmeId,
        String title,
        String description,
        LocalDate plannedStart,
        LocalDate plannedEnd,
        ActivityStatus status,
        Long assignedStaffId,
        String assignedStaffName,
        Long areaId,
        boolean overdue) {
}
