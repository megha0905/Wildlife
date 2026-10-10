package com.wildlife.resource.dto;

import com.wildlife.resource.entity.Designation;
import com.wildlife.resource.entity.StaffStatus;

/** Short form returned by GET /api/staff/lookup (CONTRACTS.md 9.2). */
public record StaffRef(Long id, String employeeCode, String fullName, Designation designation, StaffStatus status) {
}
