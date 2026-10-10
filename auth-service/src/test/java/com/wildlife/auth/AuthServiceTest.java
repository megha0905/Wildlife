package com.wildlife.auth;

import com.wildlife.auth.dto.LoginRequest;
import com.wildlife.auth.dto.LoginResponse;
import com.wildlife.auth.entity.Role;
import com.wildlife.auth.entity.User;
import com.wildlife.auth.repository.UserRepository;
import com.wildlife.auth.service.AuthService;
import com.wildlife.auth.service.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost keeps tests fast
    private UserRepository repo;
    private AuthService service;
    private User officer;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        TokenIssuer issuer = new TokenIssuer("test-secret-that-is-definitely-longer-than-32-chars", 60,
                Clock.systemDefaultZone());
        service = new AuthService(repo, encoder, issuer, Clock.systemDefaultZone());

        officer = new User();
        officer.setId(2L);
        officer.setUsername("officer");
        officer.setFullName("Anita Sharma");
        officer.setPasswordHash(encoder.encode("Officer@123"));
        officer.setRoles(Set.of(Role.FOREST_OFFICER));
        officer.setActive(true);
    }

    @Test
    void correctPasswordReturnsTokenAndRecordsLogin() {
        when(repo.findByUsernameIgnoreCase("officer")).thenReturn(Optional.of(officer));

        LoginResponse response = service.login(new LoginRequest("officer", "Officer@123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().roles()).containsExactly(Role.FOREST_OFFICER);
        assertThat(officer.getLastLoginAt()).isNotNull();
    }

    @Test
    void wrongPasswordIsRejectedWithGenericMessage() {
        when(repo.findByUsernameIgnoreCase("officer")).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> service.login(new LoginRequest("officer", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void inactiveUserCannotLogIn() {
        officer.setActive(false);
        when(repo.findByUsernameIgnoreCase("officer")).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> service.login(new LoginRequest("officer", "Officer@123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unknownUserGetsSameMessage() {
        when(repo.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "x")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
