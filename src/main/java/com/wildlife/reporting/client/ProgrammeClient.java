package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "programme", url = "${services.programme.url}")
public interface ProgrammeClient {

    @GetMapping("/api/programmes/summary")
    JsonNode summary();
}
