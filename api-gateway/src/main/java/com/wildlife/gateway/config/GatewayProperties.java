package com.wildlife.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bound from the "wildguard:" block in application.yml. */
@ConfigurationProperties(prefix = "wildguard")
public record GatewayProperties(Map<String, String> services, Cors cors) {

    public GatewayProperties {
        services = services == null ? Map.of() : new LinkedHashMap<>(services);
        // the origin list itself lives in application.yml (CORS_ALLOWED_ORIGINS)
        cors = cors == null ? new Cors(List.of()) : cors;
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
