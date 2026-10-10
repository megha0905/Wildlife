package com.wildlife.resource.dto;

import com.wildlife.resource.entity.EquipmentType;
import com.wildlife.resource.entity.ResourceStatus;

import java.time.LocalDate;

public record EquipmentResponse(
        Long id,
        String assetTag,
        String name,
        EquipmentType equipmentType,
        ResourceStatus status,
        Long homeAreaId,
        LocalDate purchasedOn,
        String notes) {
}
