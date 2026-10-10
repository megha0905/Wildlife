package com.wildlife.reporting.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wildlife.reporting.dto.DashboardResponse;
import com.wildlife.reporting.dto.ZoneReport;
import com.wildlife.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Validated
@Tag(name = "Dashboard and reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public DashboardResponse dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/incidents/monthly")
    @PreAuthorize("isAuthenticated()")
    public JsonNode monthlyIncidents(@RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
                                     @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return reportService.monthlyIncidents(year, month);
    }

    @GetMapping("/zones/{zoneId}")
    @PreAuthorize("isAuthenticated()")
    public ZoneReport zone(@PathVariable Long zoneId) {
        return reportService.zone(zoneId);
    }
}
