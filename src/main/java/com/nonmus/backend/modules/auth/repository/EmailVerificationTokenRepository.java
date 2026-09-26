package com.nonmus.backend.modules.auth.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepository {

    private static final String KEY_PREFIX = "auth:email:token:";

    private final StringRedisTemplate redisTemplate;

    public void save(String token, String email, Duration ttl) {
        String key = buildKey(token);
        log.debug("Saving verification token for email {} with ttl {}", email, ttl);
        redisTemplate.opsForValue().set(key, email, ttl);
    }

    public Optional<String> findEmailByToken(String token) {
        String key = buildKey(token);
        String email = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(email);
    }

    public Optional<String> getAndDelete(String token) {
        String key = buildKey(token);
        log.debug("Atomically retrieving and deleting verification token {}", token);
        String email = redisTemplate.opsForValue().getAndDelete(key);
        return Optional.ofNullable(email);
    }

    public boolean deleteByToken(String token) {
        String key = buildKey(token);
        log.debug("Deleting verification token {}", token);
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }
}
