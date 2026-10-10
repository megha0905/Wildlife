package com.wildlife.auth.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Uniform error body (CONTRACTS.md 3.3). */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {

    public static ApiError of(HttpStatus status, String message, String path, List<String> details) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path,
                details == null ? List.of() : details);
    }
}
