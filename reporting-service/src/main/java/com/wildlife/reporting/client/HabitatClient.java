package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "habitat", url = "${services.habitat.url}")
public interface HabitatClient {

    @GetMapping("/api/areas/summary")
    JsonNode summary();

    @GetMapping("/api/zones/{id}")
    JsonNode getZone(@PathVariable("id") Long id);

    @GetMapping("/api/water-resources")
    JsonNode waterResources(@RequestParam("zoneId") Long zoneId, @RequestParam("size") int size);
}
