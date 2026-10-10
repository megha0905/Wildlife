package com.wildlife.rescue.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wildlife", url = "${services.wildlife.url}")
public interface WildlifeClient {

    @GetMapping("/api/animals/{id}")
    AnimalDto getAnimal(@PathVariable("id") Long id);

    @PatchMapping("/api/animals/{id}/status")
    AnimalDto updateStatus(@PathVariable("id") Long id, @RequestBody AnimalStatusUpdate body);
}
