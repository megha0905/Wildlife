package com.wildlife.incident.controller;

import com.wildlife.incident.dto.AssignmentRequest;
import com.wildlife.incident.dto.AssignmentResponse;
import com.wildlife.incident.dto.IncidentDetailResponse;
import com.wildlife.incident.dto.IncidentRequest;
import com.wildlife.incident.dto.IncidentResponse;
import com.wildlife.incident.dto.IncidentSummary;
import com.wildlife.incident.dto.IncidentTransitionRequest;
import com.wildlife.incident.dto.PageResponse;
import com.wildlife.incident.dto.StatusHistoryResponse;
import com.wildlife.incident.dto.ZoneMonthlyIncidentReport;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;
import com.wildlife.incident.service.AssignmentService;
import com.wildlife.incident.service.IncidentService;
import com.wildlife.incident.service.IncidentSummaryService;
import com.wildlife.incident.service.ZoneMonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@Validated
@Tag(name = "Incidents and threats")
public class IncidentController {

    private final IncidentService incidentService;
    private final AssignmentService assignmentService;
    private final ZoneMonthlyReportService monthlyService;
    private final IncidentSummaryService summaryService;

    public IncidentController(IncidentService incidentService, AssignmentService assignmentService,
                              ZoneMonthlyReportService monthlyService, IncidentSummaryService summaryService) {
        this.incidentService = incidentService;
        this.assignmentService = assignmentService;
        this.monthlyService = monthlyService;
        this.summaryService = summaryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<IncidentResponse> search(
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return incidentService.search(type, severity, status, areaId, zoneId, from, to, q, pageable);
    }

    /** Mandatory example: all incidents in a zone for the current month, with a status summary. */
    @GetMapping("/zone/{zoneId}/current-month")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Monthly zone review: incidents in a zone this month, optional status filter")
    public ZoneMonthlyIncidentReport currentMonthInZone(@PathVariable Long zoneId,
                                                        @RequestParam(required = false) IncidentStatus status) {
        return monthlyService.currentMonth(zoneId, status);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public IncidentSummary summary(@RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
                                   @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return summaryService.summary(year, month);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public IncidentDetailResponse get(@PathVariable Long id) {
        return incidentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentRequest request) {
        IncidentResponse created = incidentService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public IncidentResponse update(@PathVariable Long id, @Valid @RequestBody IncidentRequest request) {
        return incidentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        incidentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Per-move role rules are enforced inside (CONTRACTS.md 6.1); wrong role for the move -> 403. */
    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public IncidentResponse transition(@PathVariable Long id, @Valid @RequestBody IncidentTransitionRequest request) {
        return incidentService.transition(id, request.toStatus(), request.note());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return incidentService.history(id);
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("isAuthenticated()")
    public List<AssignmentResponse> assignments(@PathVariable Long id) {
        return assignmentService.list(id);
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<AssignmentResponse> assign(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse created = assignmentService.assign(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{assignmentId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<Void> unassign(@PathVariable Long id, @PathVariable Long assignmentId) {
        assignmentService.unassign(id, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
