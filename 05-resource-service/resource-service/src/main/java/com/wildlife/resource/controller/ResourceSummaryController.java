package com.wildlife.resource.controller;

import com.wildlife.resource.dto.ResourceSummary;
import com.wildlife.resource.service.ResourceSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Resource summary")
public class ResourceSummaryController {

    private final ResourceSummaryService summaryService;

    public ResourceSummaryController(ResourceSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/api/resources/summary")
    @PreAuthorize("isAuthenticated()")
    public ResourceSummary summary() {
        return summaryService.summary();
    }
}
