package com.nonmus.backend.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;

public interface RateLimitService {
    boolean tryConsume(HttpServletRequest request);
}