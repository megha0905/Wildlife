package com.wildlife.patrol.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "incident", url = "${services.incident.url}")
public interface IncidentClient {

    @PostMapping("/api/incidents")
    IncidentDto createIncident(@RequestBody IncidentCreate body);
}
