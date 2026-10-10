package com.wildlife.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/** Checks signature, issuer and expiry only. Role checks happen in each service (CONTRACTS.md 4.3). */
@Component
public class JwtValidator {

    public static final String ISSUER = "wildguard-auth";

    private final String secret;
    private SecretKey key;

    public JwtValidator(@Value("${security.jwt.secret:}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Throws io.jsonwebtoken.JwtException (or IllegalArgumentException) when the token is not valid. */
    public void validate(String token) {
        Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build().parseSignedClaims(token);
    }
}
