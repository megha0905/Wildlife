package com.wildlife.habitat.controller;

import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.dto.ZoneRequest;
import com.wildlife.habitat.dto.ZoneResponse;
import com.wildlife.habitat.entity.ZoneType;
import com.wildlife.habitat.service.ZoneService;
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

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<ZoneResponse> search(@RequestParam(required = false) Long areaId,
                                             @RequestParam(required = false) ZoneType type,
                                             @RequestParam(required = false) String q,
                                             @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return zoneService.search(areaId, type, q, pageable);
    }

    /** Also called by wildlife, patrol, incident and reporting services (CONTRACTS.md 9.1). */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ZoneResponse get(@PathVariable Long id) {
        return zoneService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<ZoneResponse> create(@Valid @RequestBody ZoneRequest request) {
        ZoneResponse created = zoneService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ZoneResponse update(@PathVariable Long id, @Valid @RequestBody ZoneRequest request) {
        return zoneService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
