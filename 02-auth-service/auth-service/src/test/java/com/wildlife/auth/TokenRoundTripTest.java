package com.wildlife.auth;

import com.wildlife.auth.config.AuthUser;
import com.wildlife.auth.config.JwtService;
import com.wildlife.auth.entity.Role;
import com.wildlife.auth.entity.User;
import com.wildlife.auth.service.TokenIssuer;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The token auth-service issues must be readable by the JwtService every other service uses. */
class TokenRoundTripTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-chars";

    @Test
    void issuedTokenCarriesContractClaims() {
        TokenIssuer issuer = new TokenIssuer(SECRET, 120, Clock.systemDefaultZone());
        JwtService validator = new JwtService(SECRET);
        validator.init();

        User user = user(2L, "officer", "Anita Sharma", Set.of(Role.FOREST_OFFICER), 1L);
        AuthUser parsed = validator.parse(issuer.issue(user));

        assertThat(parsed.id()).isEqualTo(2L);
        assertThat(parsed.username()).isEqualTo("officer");
        assertThat(parsed.fullName()).isEqualTo("Anita Sharma");
        assertThat(parsed.roles()).containsExactly("FOREST_OFFICER");
        assertThat(parsed.staffId()).isEqualTo(1L);
    }

    @Test
    void staffIdIsNullWhenUserHasNoStaffLink() {
        TokenIssuer issuer = new TokenIssuer(SECRET, 120, Clock.systemDefaultZone());
        JwtService validator = new JwtService(SECRET);
        validator.init();

        AuthUser parsed = validator.parse(issuer.issue(user(1L, "admin", "Admin", Set.of(Role.ADMIN), null)));
        assertThat(parsed.staffId()).isNull();
    }

    @Test
    void expiredTokenIsRejected() {
        Clock longAgo = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("UTC"));
        TokenIssuer issuer = new TokenIssuer(SECRET, 120, longAgo);
        JwtService validator = new JwtService(SECRET);
        validator.init();

        String token = issuer.issue(user(3L, "ranger", "Ravi", Set.of(Role.RANGER), 2L));
        assertThatThrownBy(() -> validator.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        TokenIssuer issuer = new TokenIssuer("another-secret-that-is-also-longer-than-32", 120, Clock.systemDefaultZone());
        JwtService validator = new JwtService(SECRET);
        validator.init();

        String token = issuer.issue(user(3L, "ranger", "Ravi", Set.of(Role.RANGER), 2L));
        assertThatThrownBy(() -> validator.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRefused() {
        JwtService validator = new JwtService("too-short");
        assertThatThrownBy(validator::init).isInstanceOf(IllegalStateException.class);
    }

    private static User user(Long id, String username, String name, Set<Role> roles, Long staffId) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setFullName(name);
        u.setRoles(roles);
        u.setStaffId(staffId);
        return u;
    }
}
