package com.wildlife.resource.controller;

import com.wildlife.resource.dto.AllocationRequest;
import com.wildlife.resource.dto.AllocationResponse;
import com.wildlife.resource.dto.AllocationStatusUpdate;
import com.wildlife.resource.dto.AvailabilityResponse;
import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.entity.AllocationPurpose;
import com.wildlife.resource.entity.AllocationStatus;
import com.wildlife.resource.entity.ResourceKind;
import com.wildlife.resource.service.AllocationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PatchMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/allocations")
@Tag(name = "Allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<AllocationResponse> search(
            @RequestParam(required = false) ResourceKind resourceKind,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) AllocationPurpose purpose,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) AllocationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return allocationService.search(resourceKind, resourceId, purpose, referenceId, status, from, to, pageable);
    }

    @GetMapping("/availability")
    @PreAuthorize("isAuthenticated()")
    public AvailabilityResponse availability(
            @RequestParam ResourceKind resourceKind,
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {
        return allocationService.availability(resourceKind, resourceId, startAt, endAt);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AllocationResponse get(@PathVariable Long id) {
        return allocationService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<AllocationResponse> create(@Valid @RequestBody AllocationRequest request) {
        AllocationResponse created = allocationService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public AllocationResponse update(@PathVariable Long id, @Valid @RequestBody AllocationRequest request) {
        return allocationService.update(id, request);
    }

    /** RANGER allowed because patrol-service forwards a ranger's token when a patrol starts/ends (CONTRACTS 7.4). */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER')")
    public AllocationResponse changeStatus(@PathVariable Long id, @Valid @RequestBody AllocationStatusUpdate request) {
        return allocationService.changeStatus(id, request.status());
    }
}
