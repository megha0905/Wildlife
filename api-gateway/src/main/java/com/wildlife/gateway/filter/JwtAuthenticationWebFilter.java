package com.wildlife.gateway.filter;

import com.wildlife.gateway.config.JwtValidator;
import com.wildlife.gateway.exception.ErrorResponseWriter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Rejects /api/** requests without a valid token before they reach a service (CONTRACTS.md 4.3).
 * The Authorization header is forwarded unchanged; each service checks roles itself.
 * A WebFilter (not a GlobalFilter) so it also protects the gateway's own /api/gateway/health.
 */
@Component
@Order(-100)
public class JwtAuthenticationWebFilter implements WebFilter {

    /** Exact paths that need no token. */
    static final Set<String> PUBLIC_PATHS = Set.of("/api/auth/login");

    private final JwtValidator validator;
    private final ErrorResponseWriter writer;

    public JwtAuthenticationWebFilter(JwtValidator validator, ErrorResponseWriter writer) {
        this.validator = validator;
        this.writer = writer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!requiresToken(exchange.getRequest().getMethod(), path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return writer.write(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            validator.validate(header.substring(7).trim());
        } catch (ExpiredJwtException e) {
            return writer.write(exchange, HttpStatus.UNAUTHORIZED, "Token expired, please log in again");
        } catch (JwtException | IllegalArgumentException e) {
            return writer.write(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return chain.filter(exchange);
    }

    static boolean requiresToken(HttpMethod method, String path) {
        if (HttpMethod.OPTIONS.equals(method)) return false;   // CORS preflight
        if (!path.startsWith("/api/")) return false;           // actuator etc.
        String normalised = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        return !PUBLIC_PATHS.contains(normalised);
    }
}
