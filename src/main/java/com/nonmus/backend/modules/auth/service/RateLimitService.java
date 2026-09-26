package com.nonmus.backend.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

public interface RateLimitService {
    boolean tryConsume(HttpServletRequest request, String bucketName, long capacity, Duration refillPeriod);
}