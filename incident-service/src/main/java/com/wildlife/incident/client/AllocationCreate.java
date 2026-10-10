package com.wildlife.incident.client;

import java.time.LocalDateTime;

public record AllocationCreate(String resourceKind, Long resourceId, String purpose, Long referenceId,
                               String referenceLabel, LocalDateTime startAt, LocalDateTime endAt, String notes) {
}
