package com.wildlife.patrol.controller;

import com.wildlife.patrol.dto.EscalateRequest;
import com.wildlife.patrol.dto.LogRequest;
import com.wildlife.patrol.dto.LogResponse;
import com.wildlife.patrol.dto.PageResponse;
import com.wildlife.patrol.dto.PatrolSummary;
import com.wildlife.patrol.dto.ScheduleDetailResponse;
import com.wildlife.patrol.dto.ScheduleRequest;
import com.wildlife.patrol.dto.ScheduleResponse;
import com.wildlife.patrol.dto.ScheduleStatusUpdate;
import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.service.PatrolSummaryService;
import com.wildlife.patrol.service.ScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patrols")
@Tag(name = "Patrol schedules and logs")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final PatrolSummaryService summaryService;

    public ScheduleController(ScheduleService scheduleService, PatrolSummaryService summaryService) {
        this.scheduleService = scheduleService;
        this.summaryService = summaryService;
    }

    @GetMapping("/schedules")
    @PreAuthorize("isAuthenticated()")
    public PageResponse<ScheduleResponse> search(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) PatrolStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(sort = "patrolDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return scheduleService.search(teamId, routeId, zoneId, status, from, to, pageable);
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize("isAuthenticated()")
    public ScheduleDetailResponse get(@PathVariable Long id) {
        return scheduleService.get(id);
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse created = scheduleService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ScheduleResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(id, request);
    }

    @PatchMapping("/schedules/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER')")
    public ScheduleResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ScheduleStatusUpdate request) {
        return scheduleService.changeStatus(id, request.status(), request.note());
    }

    @GetMapping("/schedules/{id}/logs")
    @PreAuthorize("isAuthenticated()")
    public List<LogResponse> logs(@PathVariable Long id) {
        return scheduleService.logs(id);
    }

    @PostMapping("/schedules/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER')")
    public ResponseEntity<LogResponse> addLog(@PathVariable Long id, @Valid @RequestBody LogRequest request) {
        LogResponse created = scheduleService.addLog(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/patrols/logs/{logId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PostMapping("/logs/{logId}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER')")
    public LogResponse escalate(@PathVariable Long logId, @Valid @RequestBody EscalateRequest request) {
        return scheduleService.escalate(logId, request);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public PatrolSummary summary() {
        return summaryService.summary();
    }
}
