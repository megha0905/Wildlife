package com.wildlife.patrol.dto;

/** Team member as shown in responses (same field names as resource-service's StaffRef). */
public record StaffRef(Long id, String employeeCode, String fullName, String designation, String status) {
}
