package com.wildlife.resource.dto;

import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.VehicleType;

import java.time.LocalDate;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        VehicleType vehicleType,
        ResourceStatus status,
        String make,
        String model,
        Integer seatingCapacity,
        Long homeAreaId,
        LocalDate lastServicedOn) {
}
