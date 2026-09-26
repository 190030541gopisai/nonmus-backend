package com.nonmus.backend.modules.auth.repository;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCodeRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private EmailVerificationCodeRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new EmailVerificationCodeRepository(redisTemplate, objectMapper);
    }

    @Test
    void save_WhenExpiryPositive_StoresWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String email = "test@example.com";
        VerificationCodeState state = VerificationCodeState.builder()
                .code("123456")
                .usedVerifyAttempts(0)
                .maxVerifyAttempts(5)
                .expiryInSeconds(600)
                .build();

        repository.save(email, state);

        verify(valueOperations).set(eq("auth:email:code:" + email), any(String.class), eq(Duration.ofSeconds(600)));
    }

    @Test
    void findByEmail_WhenExists_ReturnsState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String email = "test@example.com";
        String json = "{\"code\":\"123456\",\"usedVerifyAttempts\":0,\"maxVerifyAttempts\":5,\"expiryInSeconds\":600}";
        when(valueOperations.get("auth:email:code:" + email)).thenReturn(json);

        VerificationCodeState state = repository.findByEmail(email);

        assertThat(state).isNotNull();
        assertThat(state.getCode()).isEqualTo("123456");
        assertThat(state.getUsedVerifyAttempts()).isZero();
    }

    @Test
    void findByEmail_WhenNotFound_ReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String email = "test@example.com";
        when(valueOperations.get("auth:email:code:" + email)).thenReturn(null);

        VerificationCodeState state = repository.findByEmail(email);

        assertThat(state).isNull();
    }

    @Test
    void deleteByEmail_DeletesKey() {
        String email = "test@example.com";

        repository.deleteByEmail(email);

        verify(redisTemplate).delete("auth:email:code:" + email);
    }

    @Test
    void verifyCode_ExecutesLuaScriptWithArguments() {
        String email = "test@example.com";
        String code = "123456";
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("auth:email:code:" + email)),
                eq(code)
        )).thenReturn(0L);

        long result = repository.verifyCode(email, code);

        assertThat(result).isEqualTo(EmailVerificationCodeRepository.VERIFY_SUCCESS);
    }
}
