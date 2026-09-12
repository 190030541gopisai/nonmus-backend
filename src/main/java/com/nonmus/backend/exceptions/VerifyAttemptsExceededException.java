package com.nonmus.backend.exceptions;

public class VerifyAttemptsExceededException extends RuntimeException {
    public VerifyAttemptsExceededException(String message) {
        super(message);
    }
}
