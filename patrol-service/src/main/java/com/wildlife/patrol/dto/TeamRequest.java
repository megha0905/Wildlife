package com.wildlife.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long areaId,
        @NotNull Long leaderStaffId,
        @NotEmpty List<@NotNull Long> memberStaffIds,
        Boolean active) {
}
