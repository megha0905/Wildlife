package com.wildlife.incident.dto;

import com.wildlife.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentTransitionRequest(@NotNull IncidentStatus toStatus, @Size(max = 2000) String note) {
}
