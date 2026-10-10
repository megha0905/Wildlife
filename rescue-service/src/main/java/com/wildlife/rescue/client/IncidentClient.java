package com.wildlife.rescue.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "incident", url = "${services.incident.url}")
public interface IncidentClient {

    @GetMapping("/api/incidents/{id}")
    IncidentDto getIncident(@PathVariable("id") Long id);
}
