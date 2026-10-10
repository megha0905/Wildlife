package com.wildlife.rescue.controller;

import com.wildlife.rescue.dto.PageResponse;
import com.wildlife.rescue.dto.RehabProgressRequest;
import com.wildlife.rescue.dto.RehabProgressResponse;
import com.wildlife.rescue.dto.RescueDetailResponse;
import com.wildlife.rescue.dto.RescueHistoryResponse;
import com.wildlife.rescue.dto.RescueRequest;
import com.wildlife.rescue.dto.RescueResponse;
import com.wildlife.rescue.dto.RescueSummary;
import com.wildlife.rescue.dto.RescueTransitionRequest;
import com.wildlife.rescue.dto.TreatmentRequest;
import com.wildlife.rescue.dto.TreatmentResponse;
import com.wildlife.rescue.entity.RescueCause;
import com.wildlife.rescue.entity.RescueStatus;
import com.wildlife.rescue.service.RescueService;
import com.wildlife.rescue.service.RescueSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rescues")
@Tag(name = "Rescue and rehabilitation")
public class RescueController {

    private final RescueService rescueService;
    private final RescueSummaryService summaryService;

    public RescueController(RescueService rescueService, RescueSummaryService summaryService) {
        this.rescueService = rescueService;
        this.summaryService = summaryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<RescueResponse> search(
            @RequestParam(required = false) RescueStatus status,
            @RequestParam(required = false) RescueCause cause,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "rescuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return rescueService.search(status, cause, areaId, zoneId, from, to, q, pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public RescueSummary summary() {
        return summaryService.summary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public RescueDetailResponse get(@PathVariable Long id) {
        return rescueService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public ResponseEntity<RescueResponse> create(@Valid @RequestBody RescueRequest request) {
        RescueResponse created = rescueService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','VET')")
    public RescueResponse update(@PathVariable Long id, @Valid @RequestBody RescueRequest request) {
        return rescueService.update(id, request);
    }

    /** Per-move role rules are enforced inside (CONTRACTS.md 6.2). */
    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','VET')")
    public RescueResponse transition(@PathVariable Long id, @Valid @RequestBody RescueTransitionRequest request) {
        return rescueService.transition(id, request);
    }

    @GetMapping("/{id}/treatments")
    @PreAuthorize("isAuthenticated()")
    public List<TreatmentResponse> treatments(@PathVariable Long id) {
        return rescueService.treatments(id);
    }

    @PostMapping("/{id}/treatments")
    @PreAuthorize("hasAnyRole('ADMIN','VET')")
    public ResponseEntity<TreatmentResponse> addTreatment(@PathVariable Long id, @Valid @RequestBody TreatmentRequest request) {
        TreatmentResponse created = rescueService.addTreatment(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{treatmentId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @GetMapping("/{id}/rehab-progress")
    @PreAuthorize("isAuthenticated()")
    public List<RehabProgressResponse> rehabProgress(@PathVariable Long id) {
        return rescueService.rehabProgress(id);
    }

    @PostMapping("/{id}/rehab-progress")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','VET')")
    public ResponseEntity<RehabProgressResponse> addRehabProgress(@PathVariable Long id,
                                                                  @Valid @RequestBody RehabProgressRequest request) {
        RehabProgressResponse created = rescueService.addRehabProgress(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{entryId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<RescueHistoryResponse> history(@PathVariable Long id) {
        return rescueService.history(id);
    }
}
