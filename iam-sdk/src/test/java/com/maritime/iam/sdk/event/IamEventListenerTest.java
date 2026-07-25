package com.maritime.iam.sdk.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maritime.iam.sdk.mapper.ApiToPageMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

class IamEventListenerTest {

    @Test
    void userInvalidationClearsNavPageAndPermissionCodes() {
        StringRedisTemplate redis = failingScanRedis();
        ApiToPageMapper mapper = mock(ApiToPageMapper.class);
        IamEventListener listener =
                new IamEventListener(redis, mapper, "TODO");

        listener.onCacheInvalidation(event(
                "TODO", List.of("user-1")));

        ArgumentCaptor<String> patterns =
                ArgumentCaptor.forClass(String.class);
        verify(redis, org.mockito.Mockito.times(3))
                .keys(patterns.capture());
        assertThat(patterns.getAllValues()).containsExactly(
                "biz:nav:TODO:user-1:*",
                "biz:page:TODO:user-1:*",
                "iam:perms:TODO:user-1:*");
        verify(mapper).refresh();
    }

    @Test
    void emptyUsersClearsEntireSystemCache() {
        StringRedisTemplate redis = failingScanRedis();
        ApiToPageMapper mapper = mock(ApiToPageMapper.class);
        IamEventListener listener =
                new IamEventListener(redis, mapper, "TODO");

        listener.onCacheInvalidation(event("TODO", List.of()));

        ArgumentCaptor<String> patterns =
                ArgumentCaptor.forClass(String.class);
        verify(redis, org.mockito.Mockito.times(3))
                .keys(patterns.capture());
        assertThat(patterns.getAllValues()).containsExactly(
                "biz:nav:TODO:*",
                "biz:page:TODO:*",
                "iam:perms:TODO:*");
    }

    @Test
    void allSystemEventAppliesToConsumer() {
        StringRedisTemplate redis = failingScanRedis();
        IamEventListener listener = new IamEventListener(
                redis, mock(ApiToPageMapper.class), "TODO");

        listener.onCacheInvalidation(
                event("ALL", List.of("user-1")));

        verify(redis, org.mockito.Mockito.times(3))
                .keys(anyString());
    }

    @Test
    void otherSystemEventIsIgnored() {
        StringRedisTemplate redis =
                mock(StringRedisTemplate.class);
        ApiToPageMapper mapper = mock(ApiToPageMapper.class);
        IamEventListener listener =
                new IamEventListener(redis, mapper, "TODO");

        listener.onCacheInvalidation(
                event("IAM", List.of("user-1")));

        verify(redis, never()).scan(any(ScanOptions.class));
        verify(mapper, never()).refresh();
    }

    private static StringRedisTemplate failingScanRedis() {
        StringRedisTemplate redis =
                mock(StringRedisTemplate.class);
        when(redis.scan(any(ScanOptions.class)))
                .thenThrow(new IllegalStateException("scan disabled"));
        when(redis.keys(anyString())).thenReturn(Set.of());
        return redis;
    }

    private static IamEventListener.CacheInvalidationEvent event(
            String systemCode, List<String> userIds) {
        return new IamEventListener.CacheInvalidationEvent(
                "event-1",
                systemCode,
                userIds,
                0,
                1,
                LocalDateTime.now());
    }
}
