package com.wildlife.patrol.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "habitat", url = "${services.habitat.url}")
public interface HabitatClient {

    @GetMapping("/api/zones/{id}")
    ZoneDto getZone(@PathVariable("id") Long id);
}
