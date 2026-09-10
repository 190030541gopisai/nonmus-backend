package com.nonmus.backend.modules.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ResendRateLimitRepository {

    private static final String COOLDOWN_KEY = "auth:resend:cooldown:";
    private static final String ATTEMPTS_KEY = "auth:resend:attempts:";

    private static final long ATTEMPTS_EXCEEDED = -999999L;

    private static final DefaultRedisScript<Long> RECORD_RESEND_SCRIPT =
            new DefaultRedisScript<>("""
                    local cooldownKey = KEYS[1]
                    local attemptsKey = KEYS[2]

                    local maxAttempts = tonumber(ARGV[1])
                    local cooldownSeconds = tonumber(ARGV[2])
                    local attemptsTtl = tonumber(ARGV[3])

                    local ttl = redis.call('TTL', cooldownKey)
                    if ttl > 0 then
                        return -ttl
                    end

                    local attempts = tonumber(redis.call('GET', attemptsKey)) or 0

                    if attempts >= maxAttempts then
                        return -999999
                    end

                    attempts = redis.call('INCR', attemptsKey)

                    if attempts == 1 then
                        redis.call('EXPIRE', attemptsKey, attemptsTtl)
                    end

                    redis.call(
                        'SET',
                        cooldownKey,
                        cooldownSeconds,
                        'EX',
                        cooldownSeconds
                    )

                    return attempts
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public long recordResend(
            String email,
            int maxAttempts,
            Duration cooldown,
            Duration attemptsTtl
    ) {
        Long result = redisTemplate.execute(
                RECORD_RESEND_SCRIPT,
                List.of(
                        cooldownKey(email),
                        attemptsKey(email)
                ),
                String.valueOf(maxAttempts),
                String.valueOf(cooldown.toSeconds()),
                String.valueOf(attemptsTtl.toSeconds())
        );

        if (result == null) {
            throw new IllegalStateException("Failed to record resend attempt.");
        }

        return result;
    }

    public static boolean isCooldownActive(long result) {
        return result < 0 && result != ATTEMPTS_EXCEEDED;
    }

    public static boolean isAttemptsExceeded(long result) {
        return result == ATTEMPTS_EXCEEDED;
    }

    public static long getRemainingCooldown(long result) {
        return -result;
    }

    private String cooldownKey(String email) {
        return COOLDOWN_KEY + email;
    }

    private String attemptsKey(String email) {
        return ATTEMPTS_KEY + email;
    }
}