package com.wildlife.auth.exception;

/** 503 Service Unavailable: a required downstream service could not be reached. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
