package com.wildlife.auth.controller;

import com.wildlife.auth.dto.ActiveRequest;
import com.wildlife.auth.dto.CreateUserRequest;
import com.wildlife.auth.dto.NewPasswordRequest;
import com.wildlife.auth.dto.PageResponse;
import com.wildlife.auth.dto.UpdateUserRequest;
import com.wildlife.auth.dto.UserResponse;
import com.wildlife.auth.entity.Role;
import com.wildlife.auth.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users (admin only)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> search(@RequestParam(required = false) Role role,
                                             @RequestParam(required = false) Boolean active,
                                             @RequestParam(required = false) String q,
                                             @PageableDefault(sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.search(role, active, q, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return userService.setActive(id, request.active());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody NewPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
