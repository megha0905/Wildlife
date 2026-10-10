package com.wildlife.patrol.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Our copy of resource-service's StaffRef (CONTRACTS.md 9.2). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StaffRefDto(Long id, String employeeCode, String fullName, String designation, String status) {
}
