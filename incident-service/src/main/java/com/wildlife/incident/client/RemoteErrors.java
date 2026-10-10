package com.wildlife.incident.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;

/** Small helpers to classify Feign failures (CONTRACTS.md 3.4). */
public final class RemoteErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RemoteErrors() {
    }

    public static boolean isNotFound(Exception e) {
        return e instanceof FeignException fe && fe.status() == 404;
    }

    public static boolean isConflict(Exception e) {
        return e instanceof FeignException fe && fe.status() == 409;
    }

    /** True for 4xx answers other than 404/409, e.g. a 422 or 403 from the other service. */
    public static boolean isClientError(Exception e) {
        return e instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500;
    }

    /** The "message" from the other service's ApiError body, or a generic text. */
    public static String messageOf(Exception e) {
        if (e instanceof FeignException fe) {
            try {
                String body = fe.contentUTF8();
                if (body != null && !body.isBlank()) {
                    JsonNode node = MAPPER.readTree(body);
                    if (node.hasNonNull("message")) {
                        return node.get("message").asText();
                    }
                }
            } catch (Exception ignored) {
                // fall through to the generic message
            }
            return fe.status() > 0 ? "HTTP " + fe.status() : "service unreachable";
        }
        return e.getClass().getSimpleName();
    }
}
