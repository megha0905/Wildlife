package com.wildlife.programme.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "resource", url = "${services.resource.url}")
public interface ResourceClient {

    @GetMapping("/api/staff/lookup")
    List<StaffRefDto> lookupStaff(@RequestParam("ids") List<Long> ids);
}
