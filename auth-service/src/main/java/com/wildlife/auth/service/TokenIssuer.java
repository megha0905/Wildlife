package com.wildlife.auth.service;

import com.wildlife.auth.config.JwtService;
import com.wildlife.auth.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Creates JWTs with the exact claims from CONTRACTS.md 4.2. Only auth-service issues tokens. */
@Service
public class TokenIssuer {

    private final SecretKey key;
    private final long expirationMinutes;
    private final Clock clock;

    public TokenIssuer(@Value("${security.jwt.secret:}") String secret,
                       @Value("${security.jwt.expiration-minutes:120}") long expirationMinutes,
                       Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.clock = clock;
    }

    public String issue(User user) {
        Instant now = clock.instant();
        List<String> roles = user.getRoles().stream().map(Enum::name).sorted().toList();
        var builder = Jwts.builder()
                .issuer(JwtService.ISSUER)
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("name", user.getFullName())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)));
        if (user.getStaffId() != null) {
            builder.claim("staffId", user.getStaffId());
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    public long expiresInSeconds() {
        return expirationMinutes * 60;
    }
}
