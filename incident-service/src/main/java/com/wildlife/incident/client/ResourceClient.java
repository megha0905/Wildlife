package com.wildlife.incident.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "resource", url = "${services.resource.url}")
public interface ResourceClient {

    @GetMapping("/api/staff/{id}")
    StaffDto getStaff(@PathVariable("id") Long id);

    @PostMapping("/api/allocations")
    AllocationDto createAllocation(@RequestBody AllocationCreate body);

    @PatchMapping("/api/allocations/{id}/status")
    AllocationDto changeAllocationStatus(@PathVariable("id") Long id, @RequestBody AllocationStatusBody body);
}
