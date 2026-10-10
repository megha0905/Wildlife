package com.wildlife.auth.dto;

import com.wildlife.auth.entity.Role;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        List<Role> roles,
        Long staffId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt) {
}
