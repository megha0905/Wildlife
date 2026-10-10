package com.wildlife.patrol.exception;

/** 409 Conflict: duplicates, illegal workflow moves, overlapping allocations, deletes while referenced. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
