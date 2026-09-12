package com.nonmus.backend.modules.auth.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailVerificationTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EmailVerificationTokenRepository(redisTemplate);
    }

    @Test
    void save_StoresTokenWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "sample-uuid-token";
        String email = "test@example.com";
        Duration ttl = Duration.ofMinutes(15);

        repository.save(token, email, ttl);

        verify(valueOperations).set("auth:email:token:" + token, email, ttl);
    }

    @Test
    void findEmailByToken_WhenTokenExists_ReturnsEmail() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "sample-uuid-token";
        String email = "test@example.com";
        when(valueOperations.get("auth:email:token:" + token)).thenReturn(email);

        Optional<String> result = repository.findEmailByToken(token);

        assertThat(result).isPresent().contains(email);
    }

    @Test
    void findEmailByToken_WhenTokenDoesNotExist_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "non-existent-token";
        when(valueOperations.get("auth:email:token:" + token)).thenReturn(null);

        Optional<String> result = repository.findEmailByToken(token);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByToken_WhenTokenDeleted_ReturnsTrue() {
        String token = "sample-uuid-token";
        when(redisTemplate.delete("auth:email:token:" + token)).thenReturn(Boolean.TRUE);

        boolean deleted = repository.deleteByToken(token);

        assertThat(deleted).isTrue();
        verify(redisTemplate).delete("auth:email:token:" + token);
    }

    @Test
    void deleteByToken_WhenTokenNotFound_ReturnsFalse() {
        String token = "non-existent-token";
        when(redisTemplate.delete("auth:email:token:" + token)).thenReturn(Boolean.FALSE);

        boolean deleted = repository.deleteByToken(token);

        assertThat(deleted).isFalse();
        verify(redisTemplate).delete("auth:email:token:" + token);
    }
}
