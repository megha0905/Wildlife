package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "resource", url = "${services.resource.url}")
public interface ResourceClient {

    @GetMapping("/api/resources/summary")
    JsonNode summary();
}
