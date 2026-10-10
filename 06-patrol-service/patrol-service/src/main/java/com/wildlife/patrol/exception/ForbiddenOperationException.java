package com.wildlife.patrol.exception;

/** 403 Forbidden for a specific operation (e.g. a workflow move this role may not make). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
