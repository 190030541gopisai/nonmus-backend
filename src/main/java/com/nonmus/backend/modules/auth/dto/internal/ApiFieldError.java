package com.nonmus.backend.modules.auth.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiFieldError {
    private final String field;
    private final String message;
}