package com.wildlife.incident.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Our copy of resource-service's StaffDto (CONTRACTS.md 9.2). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StaffDto(Long id, String employeeCode, String fullName, String designation, String status) {
}
