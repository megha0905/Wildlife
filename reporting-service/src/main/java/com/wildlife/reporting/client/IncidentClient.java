package com.wildlife.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Reporting reads other services' JSON as-is (JsonNode), so it never needs their DTO classes. */
@FeignClient(name = "incident", url = "${services.incident.url}")
public interface IncidentClient {

    @GetMapping("/api/incidents/summary")
    JsonNode summary(@RequestParam(value = "year", required = false) Integer year,
                     @RequestParam(value = "month", required = false) Integer month);

    @GetMapping("/api/incidents/zone/{zoneId}/current-month")
    JsonNode zoneCurrentMonth(@PathVariable("zoneId") Long zoneId);
}
