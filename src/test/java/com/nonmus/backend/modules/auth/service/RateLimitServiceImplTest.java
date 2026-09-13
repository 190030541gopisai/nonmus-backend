package com.nonmus.backend.modules.auth.service;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.distributed.BucketProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    @Mock
    private ProxyManager<byte[]> proxyManager;

    @Mock
    private RemoteBucketBuilder<byte[]> bucketBuilder;

    @Mock
    private BucketProxy bucket;

    private RateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitServiceImpl(proxyManager);
    }

    @Test
    void tryConsume_whenUnderLimit_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");

        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(any(byte[].class), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        boolean result = rateLimitService.tryConsume(request);
        assertTrue(result);
    }

    @Test
    void tryConsume_whenOverLimit_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(any(byte[].class), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        boolean result = rateLimitService.tryConsume(request);
        assertFalse(result);
    }
}
