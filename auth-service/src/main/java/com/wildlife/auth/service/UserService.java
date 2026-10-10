package com.wildlife.auth.service;

import com.wildlife.auth.config.CurrentUser;
import com.wildlife.auth.dto.CreateUserRequest;
import com.wildlife.auth.dto.PageResponse;
import com.wildlife.auth.dto.UpdateUserRequest;
import com.wildlife.auth.dto.UserResponse;
import com.wildlife.auth.entity.Role;
import com.wildlife.auth.entity.User;
import com.wildlife.auth.exception.BusinessRuleException;
import com.wildlife.auth.exception.ConflictException;
import com.wildlife.auth.exception.ResourceNotFoundException;
import com.wildlife.auth.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(Role role, Boolean active, String q, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        if (role != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<User, Role> roles = root.join("roles");
                return cb.equal(roles, role);
            });
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), like),
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like)));
        }
        return PageResponse.of(userRepository.findAll(spec, pageable), UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserMapper.toResponse(find(id));
    }

    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setRoles(new HashSet<>(request.roles()));
        user.setStaffId(request.staffId());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now(clock));
        return UserMapper.toResponse(userRepository.save(user));
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = find(id);
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setRoles(new HashSet<>(request.roles()));
        user.setStaffId(request.staffId());
        return UserMapper.toResponse(user);
    }

    public UserResponse setActive(Long id, boolean active) {
        User user = find(id);
        if (!active && Objects.equals(id, CurrentUser.get().id())) {
            throw new BusinessRuleException("You cannot deactivate your own account");
        }
        user.setActive(active);
        return UserMapper.toResponse(user);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = find(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
