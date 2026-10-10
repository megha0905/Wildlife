package com.wildlife.rescue.config;

import java.util.Arrays;
import java.util.List;

/** The logged-in user, built from the JWT claims (CONTRACTS.md 4.2). */
public record AuthUser(Long id, String username, String fullName, List<String> roles, Long staffId) {

    public boolean hasAnyRole(String... wanted) {
        return Arrays.stream(wanted).anyMatch(roles::contains);
    }

    /** Name to store in "reported by / changed by" snapshot columns. */
    public String displayName() {
        return fullName != null && !fullName.isBlank() ? fullName : username;
    }
}
