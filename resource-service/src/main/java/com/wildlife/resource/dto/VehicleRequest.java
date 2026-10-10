package com.wildlife.resource.dto;

import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VehicleRequest(
        @NotBlank @Size(max = 20) String registrationNumber,
        @NotNull VehicleType vehicleType,
        @NotNull ResourceStatus status,
        @Size(max = 60) String make,
        @Size(max = 60) String model,
        @Positive Integer seatingCapacity,
        Long homeAreaId,
        @PastOrPresent LocalDate lastServicedOn) {
}
