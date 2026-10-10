package com.wildlife.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
