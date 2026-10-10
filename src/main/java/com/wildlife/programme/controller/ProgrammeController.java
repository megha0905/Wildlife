package com.wildlife.programme.controller;

import com.wildlife.programme.dto.ActivityRequest;
import com.wildlife.programme.dto.ActivityResponse;
import com.wildlife.programme.dto.PageResponse;
import com.wildlife.programme.dto.ProgrammeDetailResponse;
import com.wildlife.programme.dto.ProgrammeRequest;
import com.wildlife.programme.dto.ProgrammeResponse;
import com.wildlife.programme.dto.ProgrammeStatusUpdate;
import com.wildlife.programme.dto.ProgrammeSummary;
import com.wildlife.programme.dto.ProgressUpdateRequest;
import com.wildlife.programme.dto.ProgressUpdateResponse;
import com.wildlife.programme.dto.TargetRequest;
import com.wildlife.programme.dto.TargetResponse;
import com.wildlife.programme.entity.ProgrammeStatus;
import com.wildlife.programme.entity.ProgrammeType;
import com.wildlife.programme.service.ProgrammeService;
import com.wildlife.programme.service.ProgrammeSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import java.util.List;

@RestController
@RequestMapping("/api/programmes")
@Tag(name = "Conservation programmes")
public class ProgrammeController {

    private final ProgrammeService programmeService;
    private final ProgrammeSummaryService summaryService;

    public ProgrammeController(ProgrammeService programmeService, ProgrammeSummaryService summaryService) {
        this.programmeService = programmeService;
        this.summaryService = summaryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<ProgrammeResponse> search(@RequestParam(required = false) ProgrammeStatus status,
                                                  @RequestParam(required = false) ProgrammeType type,
                                                  @RequestParam(required = false) Long areaId,
                                                  @RequestParam(required = false) String q,
                                                  @PageableDefault(sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return programmeService.search(status, type, areaId, q, pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ProgrammeSummary summary() {
        return summaryService.summary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProgrammeDetailResponse get(@PathVariable Long id) {
        return programmeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<ProgrammeResponse> create(@Valid @RequestBody ProgrammeRequest request) {
        ProgrammeResponse created = programmeService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ProgrammeResponse update(@PathVariable Long id, @Valid @RequestBody ProgrammeRequest request) {
        return programmeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ProgrammeResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ProgrammeStatusUpdate request) {
        return programmeService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programmeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- activities

    @GetMapping("/{id}/activities")
    @PreAuthorize("isAuthenticated()")
    public List<ActivityResponse> activities(@PathVariable Long id) {
        return programmeService.activities(id);
    }

    @PostMapping("/{id}/activities")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<ActivityResponse> addActivity(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        ActivityResponse created = programmeService.addActivity(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{activityId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}/activities/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER')")
    public ActivityResponse updateActivity(@PathVariable Long id, @PathVariable Long activityId,
                                           @Valid @RequestBody ActivityRequest request) {
        return programmeService.updateActivity(id, activityId, request);
    }

    @DeleteMapping("/{id}/activities/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id, @PathVariable Long activityId) {
        programmeService.deleteActivity(id, activityId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- targets

    @GetMapping("/{id}/targets")
    @PreAuthorize("isAuthenticated()")
    public List<TargetResponse> targets(@PathVariable Long id) {
        return programmeService.targets(id);
    }

    @PostMapping("/{id}/targets")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<TargetResponse> addTarget(@PathVariable Long id, @Valid @RequestBody TargetRequest request) {
        TargetResponse created = programmeService.addTarget(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{targetId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}/targets/{targetId}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public TargetResponse updateTarget(@PathVariable Long id, @PathVariable Long targetId,
                                       @Valid @RequestBody TargetRequest request) {
        return programmeService.updateTarget(id, targetId, request);
    }

    @DeleteMapping("/{id}/targets/{targetId}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<Void> deleteTarget(@PathVariable Long id, @PathVariable Long targetId) {
        programmeService.deleteTarget(id, targetId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- progress

    @GetMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public List<ProgressUpdateResponse> progress(@PathVariable Long id) {
        return programmeService.progress(id);
    }

    @PostMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public ResponseEntity<ProgressUpdateResponse> addProgress(@PathVariable Long id,
                                                              @Valid @RequestBody ProgressUpdateRequest request) {
        ProgressUpdateResponse created = programmeService.addProgress(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{updateId}").buildAndExpand(created.id()).toUri()).body(created);
    }
}
