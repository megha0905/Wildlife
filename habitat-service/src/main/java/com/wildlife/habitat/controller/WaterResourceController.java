package com.wildlife.habitat.controller;

import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.dto.WaterResourceRequest;
import com.wildlife.habitat.dto.WaterResourceResponse;
import com.wildlife.habitat.dto.WaterStatusUpdate;
import com.wildlife.habitat.entity.WaterSourceType;
import com.wildlife.habitat.entity.WaterStatus;
import com.wildlife.habitat.service.WaterResourceService;
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

@RestController
@RequestMapping("/api/water-resources")
@Tag(name = "Water resources")
public class WaterResourceController {

    private final WaterResourceService waterService;

    public WaterResourceController(WaterResourceService waterService) {
        this.waterService = waterService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<WaterResourceResponse> search(@RequestParam(required = false) Long zoneId,
                                                      @RequestParam(required = false) Long areaId,
                                                      @RequestParam(required = false) WaterSourceType type,
                                                      @RequestParam(required = false) WaterStatus status,
                                                      @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return waterService.search(zoneId, areaId, type, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public WaterResourceResponse get(@PathVariable Long id) {
        return waterService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<WaterResourceResponse> create(@Valid @RequestBody WaterResourceRequest request) {
        WaterResourceResponse created = waterService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public WaterResourceResponse update(@PathVariable Long id, @Valid @RequestBody WaterResourceRequest request) {
        return waterService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER','RANGER')")
    public WaterResourceResponse updateStatus(@PathVariable Long id, @Valid @RequestBody WaterStatusUpdate request) {
        return waterService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        waterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
