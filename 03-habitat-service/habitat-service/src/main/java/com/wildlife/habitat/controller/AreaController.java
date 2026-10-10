package com.wildlife.habitat.controller;

import com.wildlife.habitat.dto.AreaDetailResponse;
import com.wildlife.habitat.dto.AreaRequest;
import com.wildlife.habitat.dto.AreaResponse;
import com.wildlife.habitat.dto.HabitatSummary;
import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.dto.ZoneResponse;
import com.wildlife.habitat.entity.AreaType;
import com.wildlife.habitat.service.AreaService;
import com.wildlife.habitat.service.SummaryService;
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
@RequestMapping("/api/areas")
@Tag(name = "Protected areas")
public class AreaController {

    private final AreaService areaService;
    private final SummaryService summaryService;

    public AreaController(AreaService areaService, SummaryService summaryService) {
        this.areaService = areaService;
        this.summaryService = summaryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<AreaResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) AreaType type,
                                             @RequestParam(required = false) String state,
                                             @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return areaService.search(q, type, state, pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public HabitatSummary summary() {
        return summaryService.summary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AreaDetailResponse get(@PathVariable Long id) {
        return areaService.get(id);
    }

    @GetMapping("/{id}/zones")
    @PreAuthorize("isAuthenticated()")
    public List<ZoneResponse> zones(@PathVariable Long id) {
        return areaService.zonesOf(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest request) {
        AreaResponse created = areaService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return areaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        areaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
