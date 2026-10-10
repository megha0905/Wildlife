package com.wildlife.programme.dto;

import com.wildlife.programme.entity.ProgrammeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProgrammeRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull ProgrammeType programmeType,
        @NotBlank @Size(max = 1000) String objective,
        @Size(max = 2000) String description,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @DecimalMin("0.00") BigDecimal budgetInr,
        Long leadStaffId,
        List<Long> areaIds,
        List<Long> speciesIds) {
}
