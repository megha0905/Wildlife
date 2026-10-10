package com.wildlife.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Turns gateway failures into the standard error body.
 * A downstream service that is not running becomes 503 "<route> service is unavailable".
 */
@Component
@Order(-2) // before Spring Boot's default handler
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ErrorResponseWriter writer;

    public GatewayErrorHandler(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "downstream";

        if (isConnectionProblem(ex)) {
            log.warn("{} service unreachable: {}", routeId, ex.getMessage());
            return writer.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, routeId + " service is unavailable");
        }
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (status == HttpStatus.NOT_FOUND) {
                return writer.write(exchange, status,
                        "No route for " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath());
            }
            if (status == HttpStatus.SERVICE_UNAVAILABLE || status == HttpStatus.GATEWAY_TIMEOUT) {
                return writer.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, routeId + " service is unavailable");
            }
            String reason = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            return writer.write(exchange, status, reason);
        }
        log.error("Unexpected gateway error on {}", exchange.getRequest().getPath(), ex);
        return writer.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    static boolean isConnectionProblem(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof ConnectException || t instanceof UnknownHostException || t instanceof TimeoutException
                    || t.getClass().getName().contains("ConnectTimeoutException")
                    || t.getClass().getName().contains("PrematureCloseException")) {
                return true;
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return false;
    }
}
