package com.wildlife.programme.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Forwards the caller's JWT on every Feign call (CONTRACTS.md 3.5). */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String header = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (header != null) {
                    template.header(HttpHeaders.AUTHORIZATION, header);
                }
            }
        };
    }
}
