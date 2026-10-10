package com.wildlife.gateway;

import com.wildlife.gateway.config.JwtValidator;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtValidatorTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-chars";

    private String token(String issuer, long expiresInSeconds, String secret) {
        long now = System.currentTimeMillis();
        return Jwts.builder().issuer(issuer).subject("officer").claim("uid", 2).claim("roles", List.of("FOREST_OFFICER"))
                .issuedAt(new Date(now)).expiration(new Date(now + expiresInSeconds * 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    private JwtValidator validator() {
        JwtValidator v = new JwtValidator(SECRET);
        v.init();
        return v;
    }

    @Test
    void acceptsValidToken() {
        assertThatCode(() -> validator().validate(token("wildguard-auth", 600, SECRET))).doesNotThrowAnyException();
    }

    @Test
    void rejectsExpiredWrongIssuerAndWrongSecret() {
        JwtValidator v = validator();
        assertThatThrownBy(() -> v.validate(token("wildguard-auth", -10, SECRET))).isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> v.validate(token("someone-else", 600, SECRET))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> v.validate(token("wildguard-auth", 600, "a-different-secret-also-over-32-chars")))
                .isInstanceOf(JwtException.class);
    }
}
