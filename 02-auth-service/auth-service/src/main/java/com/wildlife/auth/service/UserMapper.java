package com.wildlife.auth.service;

import com.wildlife.auth.dto.UserResponse;
import com.wildlife.auth.entity.User;

final class UserMapper {

    private UserMapper() {
    }

    static UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getRoles().stream().sorted().toList(),
                u.getStaffId(),
                u.isActive(),
                u.getCreatedAt(),
                u.getLastLoginAt());
    }
}
