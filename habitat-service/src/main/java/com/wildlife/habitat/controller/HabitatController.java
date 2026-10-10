package com.wildlife.habitat.controller;

import com.wildlife.habitat.dto.HabitatRequest;
import com.wildlife.habitat.dto.HabitatResponse;
import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.entity.HabitatCondition;
import com.wildlife.habitat.entity.HabitatType;
import com.wildlife.habitat.service.HabitatService;
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
@RequestMapping("/api/habitats")
@Tag(name = "Habitats")
public class HabitatController {

    private final HabitatService habitatService;

    public HabitatController(HabitatService habitatService) {
        this.habitatService = habitatService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<HabitatResponse> search(@RequestParam(required = false) Long zoneId,
                                                @RequestParam(required = false) Long areaId,
                                                @RequestParam(required = false) HabitatType type,
                                                @RequestParam(required = false) HabitatCondition condition,
                                                @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return habitatService.search(zoneId, areaId, type, condition, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public HabitatResponse get(@PathVariable Long id) {
        return habitatService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<HabitatResponse> create(@Valid @RequestBody HabitatRequest request) {
        HabitatResponse created = habitatService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public HabitatResponse update(@PathVariable Long id, @Valid @RequestBody HabitatRequest request) {
        return habitatService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        habitatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
