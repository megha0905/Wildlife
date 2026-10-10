package com.wildlife.programme.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static access to the logged-in user inside services. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        // Only reachable from tests or background code; controllers always have a user.
        return new AuthUser(null, "system", "System", java.util.List.of(), null);
    }
}
