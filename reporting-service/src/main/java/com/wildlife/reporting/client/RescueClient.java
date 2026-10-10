package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "rescue", url = "${services.rescue.url}")
public interface RescueClient {

    @GetMapping("/api/rescues/summary")
    JsonNode summary();
}
