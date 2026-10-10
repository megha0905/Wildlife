package com.wildlife.incident.dto;

import com.wildlife.incident.entity.AssignmentRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignmentRequest(@NotNull Long staffId, @NotNull AssignmentRole assignmentRole,
                                @Size(max = 500) String note) {
}
