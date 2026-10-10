package com.wildlife.auth.dto;

import com.wildlife.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotEmpty Set<Role> roles,
        Long staffId) {
}
