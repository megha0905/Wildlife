package com.wildlife.resource.dto;

import com.wildlife.resource.entity.Designation;
import com.wildlife.resource.entity.StaffStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StaffRequest(
        @NotBlank @Size(max = 20) String employeeCode,
        @NotBlank @Size(max = 120) String fullName,
        @NotNull Designation designation,
        @NotNull StaffStatus status,
        @Pattern(regexp = "^[0-9+ -]{6,20}$", message = "must be a phone number") String phone,
        @Email @Size(max = 150) String email,
        Long homeAreaId,
        @PastOrPresent LocalDate joinedOn) {
}
