package com.wildlife.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationWebFilterTest {

    @Test
    void loginAndPreflightAndActuatorArePublic() {
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.POST, "/api/auth/login")).isFalse();
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.POST, "/api/auth/login/")).isFalse();
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.OPTIONS, "/api/incidents")).isFalse();
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.GET, "/actuator/health")).isFalse();
    }

    @Test
    void everythingElseUnderApiNeedsAToken() {
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.GET, "/api/auth/me")).isTrue();
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.GET, "/api/incidents/zone/1/current-month")).isTrue();
        assertThat(JwtAuthenticationWebFilter.requiresToken(HttpMethod.GET, "/api/gateway/health")).isTrue();
    }
}
