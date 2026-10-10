package com.wildlife.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewPasswordRequest(@NotBlank @Size(min = 8, max = 100) String newPassword) {
}
