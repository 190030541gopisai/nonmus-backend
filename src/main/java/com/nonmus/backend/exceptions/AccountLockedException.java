package com.nonmus.backend.exceptions;

import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {
    private final long retryAfterSeconds;

    public AccountLockedException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}