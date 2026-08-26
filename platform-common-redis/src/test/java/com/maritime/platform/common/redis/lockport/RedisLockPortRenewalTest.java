package com.maritime.platform.common.redis.lockport;

import com.maritime.platform.common.redis.lockport.LockPort.LockHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockPortRenewalTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisLockPort lockPort;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lockPort = new RedisLockPort(redisTemplate, "platform:lock");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void renew_whenTokenStillOwnsLock_extendsTtlWithOwnershipCheck() {
        when(valueOperations.setIfAbsent(
                any(String.class), any(String.class), any(Long.class), any(TimeUnit.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        Optional<LockHandle> handle = lockPort.tryLock(
                "leader", "retention", Duration.ZERO, Duration.ofSeconds(10));

        assertThat(handle).isPresent();
        assertThat(handle.get().renew(Duration.ofSeconds(5))).isTrue();

        ArgumentCaptor<RedisScript> script = ArgumentCaptor.forClass(RedisScript.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        org.mockito.Mockito.verify(redisTemplate).execute(
                script.capture(),
                org.mockito.ArgumentMatchers.eq(
                        Collections.singletonList("platform:lock:leader:retention")),
                arguments.capture());

        assertThat(script.getValue().getScriptAsString())
                .contains("redis.call('get', KEYS[1]) == ARGV[1]")
                .contains("redis.call('pexpire', KEYS[1], ARGV[2])");
        assertThat(arguments.getValue()).hasSize(2);
        assertThat(arguments.getValue()[1]).isEqualTo("5000");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void renew_whenTokenNoLongerOwnsLock_returnsFalse() {
        when(valueOperations.setIfAbsent(
                any(String.class), any(String.class), any(Long.class), any(TimeUnit.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);

        LockHandle handle = lockPort.tryLock(
                "leader", "retention", Duration.ZERO, Duration.ofSeconds(10)).orElseThrow();

        assertThat(handle.renew(Duration.ofSeconds(5))).isFalse();
    }
}
