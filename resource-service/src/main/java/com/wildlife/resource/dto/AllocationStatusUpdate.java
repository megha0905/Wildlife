package com.wildlife.resource.dto;

import com.wildlife.resource.entity.AllocationStatus;
import jakarta.validation.constraints.NotNull;

public record AllocationStatusUpdate(@NotNull AllocationStatus status) {
}
