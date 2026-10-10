package com.wildlife.patrol.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Validates tokens issued by auth-service (HS256, secret from JWT_SECRET). */
@Service
public class JwtService {

    public static final String ISSUER = "wildguard-auth";

    private final String secret;
    private SecretKey key;

    public JwtService(@Value("${security.jwt.secret:}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Parses and validates a token. Throws io.jsonwebtoken.JwtException if invalid or expired. */
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number uid = claims.get("uid", Number.class);
        Number staffId = claims.get("staffId", Number.class);
        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null ? List.of() : rawRoles.stream().map(String::valueOf).toList();

        return new AuthUser(
                uid == null ? null : uid.longValue(),
                claims.getSubject(),
                claims.get("name", String.class),
                roles,
                staffId == null ? null : staffId.longValue());
    }
}
