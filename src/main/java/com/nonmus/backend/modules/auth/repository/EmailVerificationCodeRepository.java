package com.nonmus.backend.modules.auth.repository;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailVerificationCodeRepository {

    private static final String KEY_PREFIX = "auth:email:code:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public VerificationCodeState save(String email, VerificationCodeState state) {
        try {
            String key = key(email);
            String json = objectMapper.writeValueAsString(state);

            if (state.getExpiryInSeconds() > 0) {
                redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(state.getExpiryInSeconds()));
            } else {
                redisTemplate.opsForValue().set(key, json);
            }

            return state;
        } catch (Exception e) {
            log.error("Failed to save verification code state for email: {}", email, e);
            throw new IllegalStateException("Failed to save verification code state in Redis", e);
        }
    }

    public VerificationCodeState findByEmail(String email) {
        try {
            String key = key(email);
            String json = redisTemplate.opsForValue().get(key);

            if (json == null || json.isBlank()) {
                return null;
            }

            return objectMapper.readValue(json, VerificationCodeState.class);
        } catch (Exception e) {
            log.error("Failed to deserialize verification code state for email: {}", email, e);
            throw new IllegalStateException("Failed to read verification code state from Redis", e);
        }
    }

    public void deleteByEmail(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }
}
