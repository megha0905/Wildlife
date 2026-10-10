package com.wildlife.programme.exception;

/** 422 Unprocessable Entity: valid input that breaks a business rule. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
