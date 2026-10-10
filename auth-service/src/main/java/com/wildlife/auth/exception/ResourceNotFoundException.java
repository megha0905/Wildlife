package com.wildlife.auth.exception;

/** 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " " + id + " not found");
    }
}
