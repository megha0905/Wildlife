package com.wildlife.resource.dto;

import com.wildlife.resource.entity.ResourceKind;

import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityResponse(
        ResourceKind resourceKind,
        Long resourceId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean available,
        List<AllocationResponse> conflicts) {
}
