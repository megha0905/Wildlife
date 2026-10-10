package com.wildlife.patrol.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wildlife", url = "${services.wildlife.url}")
public interface WildlifeClient {

    @GetMapping("/api/animals/tag/{tagId}")
    AnimalDto getByTag(@PathVariable("tagId") String tagId);

    @PatchMapping("/api/animals/{id}/sighting")
    AnimalDto recordSighting(@PathVariable("id") Long id, @RequestBody SightingUpdate body);
}
