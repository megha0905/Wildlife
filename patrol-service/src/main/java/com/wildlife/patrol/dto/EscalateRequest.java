package com.wildlife.patrol.dto;

import com.wildlife.patrol.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EscalateRequest(
        @NotNull @Pattern(regexp = "WILDLIFE_INJURY|HUMAN_ANIMAL_CONFLICT|POACHING|ILLEGAL_LOGGING|ENCROACHMENT|FOREST_FIRE|OTHER_THREAT",
                message = "must be an IncidentType") String incidentType,
        @NotNull Severity severity,
        @NotBlank @Size(max = 150) String title) {
}
