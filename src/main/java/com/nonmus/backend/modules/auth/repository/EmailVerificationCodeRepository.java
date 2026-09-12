package com.nonmus.backend.modules.auth.repository;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailVerificationCodeRepository {

    public static final long VERIFY_SUCCESS = 0L;
    public static final long VERIFY_NOT_FOUND = -1L;
    public static final long VERIFY_ATTEMPTS_EXCEEDED = -2L;

    private static final String KEY_PREFIX = "auth:email:code:";

    private static final DefaultRedisScript<Long> VERIFY_CODE_SCRIPT =
            new DefaultRedisScript<>("""
                    local key = KEYS[1]
                    local submittedCode = tostring(ARGV[1])

                    local raw = redis.call('GET', key)
                    if not raw then
                        return -1
                    end

                    local state = cjson.decode(raw)
                    local usedAttempts = tonumber(state['usedVerifyAttempts']) or 0
                    local maxAttempts = tonumber(state['maxVerifyAttempts']) or 5
                    local actualCode = tostring(state['code'])

                    if usedAttempts >= maxAttempts then
                        redis.call('DEL', key)
                        return -2
                    end

                    if actualCode == submittedCode then
                        redis.call('DEL', key)
                        return 0
                    end

                    usedAttempts = usedAttempts + 1
                    state['usedVerifyAttempts'] = usedAttempts

                    if usedAttempts >= maxAttempts then
                        redis.call('DEL', key)
                        return -2
                    end

                    local ttl = redis.call('TTL', key)
                    local newJson = cjson.encode(state)
                    if ttl > 0 then
                        redis.call('SET', key, newJson, 'EX', ttl)
                    else
                        redis.call('SET', key, newJson)
                    end

                    return maxAttempts - usedAttempts
                    """,
                    Long.class
            );

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

    public long verifyCode(String email, String code) {
        log.debug("Executing atomic verification Lua script for email: {}", email);
        try {
            Long result = redisTemplate.execute(
                    VERIFY_CODE_SCRIPT,
                    List.of(key(email)),
                    code
            );

            if (result == null) {
                log.error("Redis atomic verify script returned null for email: {}", email);
                throw new IllegalStateException("Failed to execute atomic verification in Redis");
            }

            return result;
        } catch (Exception e) {
            log.error("Error executing atomic verification for email: {}", email, e);
            throw new IllegalStateException("Failed to verify code in Redis", e);
        }
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }
}
