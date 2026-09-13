package com.nonmus.backend.exceptions;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
