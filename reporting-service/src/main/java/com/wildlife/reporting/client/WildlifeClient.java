package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "wildlife", url = "${services.wildlife.url}")
public interface WildlifeClient {

    @GetMapping("/api/animals/summary")
    JsonNode summary();

    @GetMapping("/api/animals")
    JsonNode animals(@RequestParam("zoneId") Long zoneId, @RequestParam("size") int size);
}
