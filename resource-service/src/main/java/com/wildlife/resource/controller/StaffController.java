package com.wildlife.resource.controller;

import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.dto.StaffRef;
import com.wildlife.resource.dto.StaffRequest;
import com.wildlife.resource.dto.StaffResponse;
import com.wildlife.resource.entity.Designation;
import com.wildlife.resource.entity.StaffStatus;
import com.wildlife.resource.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Field staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<StaffResponse> search(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Designation designation,
                                              @RequestParam(required = false) StaffStatus status,
                                              @RequestParam(required = false) Long areaId,
                                              @PageableDefault(sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return staffService.search(q, designation, status, areaId, pageable);
    }

    /** Batch lookup: GET /api/staff/lookup?ids=1,2,3 (used by patrol and programme services). */
    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    public List<StaffRef> lookup(@RequestParam List<Long> ids) {
        return staffService.lookup(ids);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public StaffResponse get(@PathVariable Long id) {
        return staffService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest request) {
        StaffResponse created = staffService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return staffService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
