package com.wildlife.rescue.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TreatmentResponse(
        Long id,
        Long rescueId,
        LocalDate treatmentDate,
        String diagnosis,
        String procedure,
        String medications,
        Long vetStaffId,
        String vetName,
        String notes,
        LocalDateTime createdAt) {
}
