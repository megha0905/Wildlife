package com.wildlife.auth.dto;

import com.wildlife.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may contain letters, digits, '.', '_' and '-' only")
        String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotEmpty Set<Role> roles,
        Long staffId) {
}
