package com.nonmus.backend.exceptions;

public class ResendAttemptsExceededException extends RuntimeException {
    public ResendAttemptsExceededException(String message) {
        super(message);
    }
}
