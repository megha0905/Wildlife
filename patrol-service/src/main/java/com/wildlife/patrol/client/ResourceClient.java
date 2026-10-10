package com.wildlife.patrol.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "resource", url = "${services.resource.url}")
public interface ResourceClient {

    @GetMapping("/api/staff/lookup")
    List<StaffRefDto> lookupStaff(@RequestParam("ids") List<Long> ids);

    @PostMapping("/api/allocations")
    AllocationDto createAllocation(@RequestBody AllocationCreate body);

    @PatchMapping("/api/allocations/{id}/status")
    AllocationDto changeAllocationStatus(@PathVariable("id") Long id, @RequestBody AllocationStatusBody body);
}
