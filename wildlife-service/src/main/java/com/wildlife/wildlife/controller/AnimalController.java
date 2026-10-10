package com.wildlife.wildlife.controller;

import com.wildlife.wildlife.dto.AnimalRequest;
import com.wildlife.wildlife.dto.AnimalResponse;
import com.wildlife.wildlife.dto.AnimalStatusUpdate;
import com.wildlife.wildlife.dto.HealthRecordRequest;
import com.wildlife.wildlife.dto.HealthRecordResponse;
import com.wildlife.wildlife.dto.PageResponse;
import com.wildlife.wildlife.dto.SightingUpdate;
import com.wildlife.wildlife.dto.WildlifeSummary;
import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.HealthStatus;
import com.wildlife.wildlife.service.AnimalService;
import com.wildlife.wildlife.service.WildlifeSummaryService;
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
@RequestMapping("/api/animals")
@Tag(name = "Animals")
public class AnimalController {

    private final AnimalService animalService;
    private final WildlifeSummaryService summaryService;

    public AnimalController(AnimalService animalService, WildlifeSummaryService summaryService) {
        this.animalService = animalService;
        this.summaryService = summaryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<AnimalResponse> search(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Long speciesId,
                                               @RequestParam(required = false) HealthStatus healthStatus,
                                               @RequestParam(required = false) AnimalStatus status,
                                               @RequestParam(required = false) Long areaId,
                                               @RequestParam(required = false) Long zoneId,
                                               @PageableDefault(sort = "tagId", direction = Sort.Direction.ASC) Pageable pageable) {
        return animalService.search(q, speciesId, healthStatus, status, areaId, zoneId, pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public WildlifeSummary summary() {
        return summaryService.summary();
    }

    @GetMapping("/tag/{tagId}")
    @PreAuthorize("isAuthenticated()")
    public AnimalResponse byTag(@PathVariable String tagId) {
        return animalService.getByTag(tagId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AnimalResponse get(@PathVariable Long id) {
        return animalService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public ResponseEntity<AnimalResponse> create(@Valid @RequestBody AnimalRequest request) {
        AnimalResponse created = animalService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public AnimalResponse update(@PathVariable Long id, @Valid @RequestBody AnimalRequest request) {
        return animalService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public AnimalResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AnimalStatusUpdate request) {
        return animalService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/sighting")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER','VET')")
    public AnimalResponse recordSighting(@PathVariable Long id, @Valid @RequestBody SightingUpdate request) {
        return animalService.recordSighting(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        animalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/health-records")
    @PreAuthorize("isAuthenticated()")
    public List<HealthRecordResponse> healthRecords(@PathVariable Long id) {
        return animalService.healthRecords(id);
    }

    @PostMapping("/{id}/health-records")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','VET')")
    public ResponseEntity<HealthRecordResponse> addHealthRecord(@PathVariable Long id,
                                                                @Valid @RequestBody HealthRecordRequest request) {
        HealthRecordResponse created = animalService.addHealthRecord(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{recordId}").buildAndExpand(created.id()).toUri()).body(created);
    }
}
