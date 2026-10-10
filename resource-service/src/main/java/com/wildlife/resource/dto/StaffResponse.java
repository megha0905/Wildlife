package com.wildlife.resource.dto;

import com.wildlife.resource.entity.Designation;
import com.wildlife.resource.entity.StaffStatus;

import java.time.LocalDate;

/** Also the cross-service StaffDto (CONTRACTS.md 9.2). Do not rename fields. */
public record StaffResponse(
        Long id,
        String employeeCode,
        String fullName,
        Designation designation,
        StaffStatus status,
        String phone,
        String email,
        Long homeAreaId,
        LocalDate joinedOn) {
}
