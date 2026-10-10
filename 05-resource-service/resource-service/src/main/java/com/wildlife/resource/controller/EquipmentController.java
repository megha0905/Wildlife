package com.wildlife.resource.controller;

import com.wildlife.resource.dto.EquipmentRequest;
import com.wildlife.resource.dto.EquipmentResponse;
import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.entity.EquipmentType;
import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.service.FleetService;
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
@RequestMapping("/api/equipment")
@Tag(name = "Equipment")
public class EquipmentController {

    private final FleetService fleetService;

    public EquipmentController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<EquipmentResponse> search(@RequestParam(required = false) EquipmentType type,
                                                  @RequestParam(required = false) ResourceStatus status,
                                                  @RequestParam(required = false) Long areaId,
                                                  @PageableDefault(sort = "assetTag", direction = Sort.Direction.ASC) Pageable pageable) {
        return fleetService.searchEquipment(type, status, areaId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EquipmentResponse get(@PathVariable Long id) {
        return fleetService.getEquipment(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse created = fleetService.createEquipment(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return fleetService.updateEquipment(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fleetService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }
}
