package com.wildlife.resource.dto;

import com.wildlife.resource.entity.AllocationPurpose;
import com.wildlife.resource.entity.AllocationStatus;
import com.wildlife.resource.entity.ResourceKind;

import java.time.LocalDateTime;

public record AllocationResponse(
        Long id,
        ResourceKind resourceKind,
        Long resourceId,
        String resourceLabel,
        AllocationPurpose purpose,
        Long referenceId,
        String referenceLabel,
        LocalDateTime startAt,
        LocalDateTime endAt,
        AllocationStatus status,
        Long allocatedByUserId,
        String allocatedByName,
        String notes,
        LocalDateTime createdAt) {
}
