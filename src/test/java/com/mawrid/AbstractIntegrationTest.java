package com.mawrid;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Base class for integration tests.
 * Uses H2 in-memory DB (no Docker needed) and mocks Redis + external services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** Mock Redis so JWT blacklist works without a real Redis server */
    @MockBean
    StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpRedisMock() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        // hasKey returning false means no token is blacklisted
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
    }
}
