package com.nonmus.backend.modules.auth.service;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final ProxyManager<byte[]> proxyManager;

    @Override
    public boolean tryConsume(HttpServletRequest request, String bucketName, long capacity, Duration refillPeriod) {
        String clientIp = extractClientIp(request);
        String bucketKeyStr = "rate_limit_" + bucketName + "_" + clientIp;
        byte[] bucketKey = bucketKeyStr.getBytes(StandardCharsets.UTF_8);

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, refillPeriod))
                .build();

        return proxyManager.builder().build(bucketKey, configSupplier).tryConsume(1);
    }

    private String extractClientIp(HttpServletRequest request) {
        // Delegate to container Request (which respects server.forward-headers-strategy)
        // Manual X-Forwarded-For parsing here would allow trivial IP spoofing.
        return request.getRemoteAddr();
    }
}


