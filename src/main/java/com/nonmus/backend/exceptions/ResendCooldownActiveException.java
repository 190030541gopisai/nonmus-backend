package com.nonmus.backend.exceptions;

import lombok.Getter;

@Getter
public class ResendCooldownActiveException extends RuntimeException {

    private final long retryAfterSeconds;

    public ResendCooldownActiveException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
