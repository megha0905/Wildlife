package com.wildlife.wildlife.controller;

import com.wildlife.wildlife.dto.PageResponse;
import com.wildlife.wildlife.dto.SpeciesRequest;
import com.wildlife.wildlife.dto.SpeciesResponse;
import com.wildlife.wildlife.entity.ConservationStatus;
import com.wildlife.wildlife.entity.SpeciesCategory;
import com.wildlife.wildlife.service.SpeciesService;
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
@RequestMapping("/api/species")
@Tag(name = "Species")
public class SpeciesController {

    private final SpeciesService speciesService;

    public SpeciesController(SpeciesService speciesService) {
        this.speciesService = speciesService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<SpeciesResponse> search(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) SpeciesCategory category,
                                                @RequestParam(required = false) ConservationStatus conservationStatus,
                                                @PageableDefault(sort = "commonName", direction = Sort.Direction.ASC) Pageable pageable) {
        return speciesService.search(q, category, conservationStatus, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public SpeciesResponse get(@PathVariable Long id) {
        return speciesService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<SpeciesResponse> create(@Valid @RequestBody SpeciesRequest request) {
        SpeciesResponse created = speciesService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public SpeciesResponse update(@PathVariable Long id, @Valid @RequestBody SpeciesRequest request) {
        return speciesService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        speciesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
