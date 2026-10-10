package com.wildlife.resource.controller;

import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.dto.VehicleRequest;
import com.wildlife.resource.dto.VehicleResponse;
import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.VehicleType;
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
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles")
public class VehicleController {

    private final FleetService fleetService;

    public VehicleController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<VehicleResponse> search(@RequestParam(required = false) VehicleType type,
                                                @RequestParam(required = false) ResourceStatus status,
                                                @RequestParam(required = false) Long areaId,
                                                @PageableDefault(sort = "registrationNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return fleetService.searchVehicles(type, status, areaId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public VehicleResponse get(@PathVariable Long id) {
        return fleetService.getVehicle(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse created = fleetService.createVehicle(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return fleetService.updateVehicle(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fleetService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
