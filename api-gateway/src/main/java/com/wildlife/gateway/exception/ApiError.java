package com.wildlife.gateway.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Same error body as every service (CONTRACTS.md 3.3). */
public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path,
                       List<String> details) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, List.of());
    }
}
