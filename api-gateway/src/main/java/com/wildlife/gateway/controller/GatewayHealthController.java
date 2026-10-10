package com.wildlife.gateway.controller;

import com.wildlife.gateway.config.GatewayProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/gateway/health: UP/DOWN for every service, used by the frontend status indicator. */
@RestController
public class GatewayHealthController {

    private final GatewayProperties properties;
    private final WebClient webClient;

    public GatewayHealthController(GatewayProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    @GetMapping("/api/gateway/health")
    public Mono<Map<String, Object>> health() {
        return Flux.fromIterable(properties.services().entrySet())
                .flatMap(entry -> webClient.get()
                        .uri(entry.getValue() + "/actuator/health")
                        .retrieve()
                        .toBodilessEntity()
                        .map(r -> Map.entry(entry.getKey(), "UP"))
                        .timeout(Duration.ofSeconds(2))
                        .onErrorReturn(Map.entry(entry.getKey(), "DOWN")))
                .collectList()
                .map(list -> {
                    Map<String, String> services = new LinkedHashMap<>();
                    list.stream()
                            .sorted(Comparator.comparing(e -> order(e.getKey())))
                            .forEach(e -> services.put(e.getKey(), e.getValue()));
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("services", services);
                    return body;
                });
    }

    private int order(String name) {
        int i = 0;
        for (String key : properties.services().keySet()) {
            if (key.equals(name)) return i;
            i++;
        }
        return i;
    }
}
