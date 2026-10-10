package com.wildlife.rescue.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RehabProgressResponse(
        Long id,
        Long rescueId,
        LocalDate assessedOn,
        String enclosure,
        String behaviourNotes,
        Double weightKg,
        boolean fitForRelease,
        String assessedBy,
        LocalDateTime createdAt) {
}
