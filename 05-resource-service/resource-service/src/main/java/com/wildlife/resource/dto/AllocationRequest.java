package com.wildlife.resource.dto;

import com.wildlife.resource.entity.AllocationPurpose;
import com.wildlife.resource.entity.ResourceKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Also sent by patrol-service and incident-service as AllocationCreate (CONTRACTS.md 9.2). */
public record AllocationRequest(
        @NotNull ResourceKind resourceKind,
        @NotNull Long resourceId,
        @NotNull AllocationPurpose purpose,
        Long referenceId,
        @Size(max = 200) String referenceLabel,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @Size(max = 1000) String notes) {
}
