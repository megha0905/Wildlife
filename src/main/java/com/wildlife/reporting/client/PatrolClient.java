package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "patrol", url = "${services.patrol.url}")
public interface PatrolClient {

    @GetMapping("/api/patrols/summary")
    JsonNode summary();
}
