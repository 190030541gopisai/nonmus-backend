package com.nonmus.backend.modules.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nonmus.backend.modules.auth.dto.internal.ApiFieldError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final Long retryAfterSeconds;

    @Builder.Default
    private final List<ApiFieldError> fieldErrors = List.of();
}