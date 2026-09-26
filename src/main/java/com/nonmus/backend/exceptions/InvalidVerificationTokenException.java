package com.nonmus.backend.exceptions;

public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String message) {
        super(message);
    }
}
