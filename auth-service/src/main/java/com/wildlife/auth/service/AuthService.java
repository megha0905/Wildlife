package com.wildlife.auth.service;

import com.wildlife.auth.config.CurrentUser;
import com.wildlife.auth.dto.ChangePasswordRequest;
import com.wildlife.auth.dto.LoginRequest;
import com.wildlife.auth.dto.LoginResponse;
import com.wildlife.auth.dto.UserResponse;
import com.wildlife.auth.entity.User;
import com.wildlife.auth.exception.BusinessRuleException;
import com.wildlife.auth.exception.ResourceNotFoundException;
import com.wildlife.auth.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenIssuer tokenIssuer, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    /** Same message for unknown user, wrong password and inactive user, so usernames can't be probed. */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        user.setLastLoginAt(LocalDateTime.now(clock));
        String token = tokenIssuer.issue(user);
        return new LoginResponse(token, "Bearer", tokenIssuer.expiresInSeconds(), UserMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        Long id = CurrentUser.get().id();
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public void changePassword(ChangePasswordRequest request) {
        Long id = CurrentUser.get().id();
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessRuleException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
