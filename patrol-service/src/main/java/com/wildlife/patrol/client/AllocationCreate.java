package com.wildlife.patrol.client;

import java.time.LocalDateTime;

/** Body for resource-service POST /api/allocations (CONTRACTS.md 9.2). */
public record AllocationCreate(String resourceKind, Long resourceId, String purpose, Long referenceId,
                               String referenceLabel, LocalDateTime startAt, LocalDateTime endAt, String notes) {
}
