package com.wildlife.resource.dto;

import com.wildlife.resource.entity.EquipmentType;
import com.wildlife.resource.entity.ResourceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EquipmentRequest(
        @NotBlank @Size(max = 30) String assetTag,
        @NotBlank @Size(max = 120) String name,
        @NotNull EquipmentType equipmentType,
        @NotNull ResourceStatus status,
        Long homeAreaId,
        @PastOrPresent LocalDate purchasedOn,
        @Size(max = 1000) String notes) {
}
