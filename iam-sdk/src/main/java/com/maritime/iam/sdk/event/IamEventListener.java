package com.maritime.iam.sdk.event;

import com.maritime.iam.sdk.mapper.ApiToPageMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * MQ event listener that consumes cache invalidation
 * notifications from iam-event-service and clears local
 * L2 Redis cache.
 *
 * <p>Conditionally registered when
 * {@code iam.event.enabled=true}.
 */
public class IamEventListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(IamEventListener.class);

    private final StringRedisTemplate redisTemplate;
    private final ApiToPageMapper apiToPageMapper;
    private final String systemCode;

    public IamEventListener(
            StringRedisTemplate redisTemplate,
            ApiToPageMapper apiToPageMapper,
            String systemCode) {
        this.redisTemplate = redisTemplate;
        this.apiToPageMapper = apiToPageMapper;
        this.systemCode = systemCode;
    }

    @RabbitListener(queues = "#{iamSdkCacheInvalidationQueue.name}")
    public void onCacheInvalidation(CacheInvalidationEvent event) {
        if (!systemCode.equals(event.systemCode())) {
            return;
        }
        LOG.info("Cache invalidation received: userCount={}",
                event.userIds() != null
                        ? event.userIds().size() : 0);
        clearL2Cache(event.userIds());
        apiToPageMapper.refresh();
    }

    private void clearL2Cache(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (String userId : userIds) {
            clearUserNavCache(userId);
            clearUserPageCache(userId);
        }
    }

    private void clearUserNavCache(String userId) {
        String pattern = "biz:nav:" + systemCode
                + ":" + userId + ":*";
        scanAndDelete(pattern);
    }

    private void clearUserPageCache(String userId) {
        String pattern = "biz:page:" + systemCode
                + ":" + userId + ":*";
        scanAndDelete(pattern);
    }

    /**
     * SCAN-based deletion. Non-blocking alternative to KEYS.
     */
    private void scanAndDelete(String pattern) {
        try (var cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions
                        .scanOptions().match(pattern)
                        .count(100).build())) {
            java.util.List<String> batch =
                    new java.util.ArrayList<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 100) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
        } catch (Exception e) {
            // Fallback: cursor may fail in some Redis configs
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    /**
     * Cache invalidation event from iam-event-service
     * (Fanout exchange).
     */
    public record CacheInvalidationEvent(
            String eventId,
            String systemCode,
            List<String> userIds,
            int batchIndex,
            int totalBatches,
            LocalDateTime occurredAt
    ) {
    }
}
