package com.maritime.platform.common.redis.leader;

import com.maritime.platform.common.redis.lockport.LockPort;
import com.maritime.platform.common.redis.lockport.LockPort.LockHandle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AOP interceptor implementing the {@link LeaderElected} contract. Acquires the
 * named distributed lock via {@link LockPort} before invoking the method body;
 * if the lock is held by another instance, skips the call (returns {@code null}
 * for reference returns; void methods proceed as no-ops).
 */
@Aspect
public class LeaderElectedAspect implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectedAspect.class);
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

    private final LockPort lockPort;
    private final ScheduledExecutorService renewalExecutor;

    public LeaderElectedAspect(LockPort lockPort) {
        this(lockPort, Executors.newScheduledThreadPool(2, renewalThreadFactory()));
    }

    LeaderElectedAspect(LockPort lockPort, ScheduledExecutorService renewalExecutor) {
        this.lockPort = lockPort;
        this.renewalExecutor = renewalExecutor;
    }

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint pjp, LeaderElected annotation) throws Throwable {
        Optional<LockHandle> handle = lockPort.tryLock(
                "leader",
                annotation.name(),
                Duration.ofMillis(annotation.waitMillis()),
                Duration.ofMillis(annotation.leaseMillis()));

        if (handle.isEmpty()) {
            if (annotation.silentSkip()) {
                log.debug("@LeaderElected[{}] skipped — another instance holds the lock", annotation.name());
            } else {
                log.info("@LeaderElected[{}] skipped — another instance holds the lock", annotation.name());
            }
            return null;
        }

        LockHandle acquiredHandle = handle.get();
        Duration leaseTime = Duration.ofMillis(annotation.leaseMillis());
        AtomicBoolean leaseLost = new AtomicBoolean(false);

        try (acquiredHandle) {
            ScheduledFuture<?> renewal = scheduleRenewal(
                    acquiredHandle, annotation, leaseTime, leaseLost);
            try {
                Object result = pjp.proceed();
                stopRenewal(renewal);
                renewal = null;

                if (annotation.renewLease()
                        && (leaseLost.get() || !renew(acquiredHandle, leaseTime, annotation.name()))) {
                    throw new IllegalStateException(
                            "@LeaderElected[" + annotation.name()
                                    + "] lost lock ownership during execution");
                }
                return result;
            } finally {
                stopRenewal(renewal);
            }
        }
    }

    private ScheduledFuture<?> scheduleRenewal(
            LockHandle handle,
            LeaderElected annotation,
            Duration leaseTime,
            AtomicBoolean leaseLost) {
        if (!annotation.renewLease()) {
            return null;
        }
        if (annotation.leaseMillis() <= 0) {
            throw new IllegalArgumentException("@LeaderElected leaseMillis must be positive");
        }

        long intervalMillis = Math.max(1L, annotation.leaseMillis() / 3L);
        return renewalExecutor.scheduleAtFixedRate(() -> {
            if (leaseLost.get()) {
                return;
            }
            if (!renew(handle, leaseTime, annotation.name())) {
                leaseLost.set(true);
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private boolean renew(LockHandle handle, Duration leaseTime, String leaderName) {
        try {
            boolean renewed = handle.renew(leaseTime);
            if (!renewed) {
                log.error("@LeaderElected[{}] lost lock ownership while renewing its lease", leaderName);
            }
            return renewed;
        } catch (RuntimeException exception) {
            log.error("@LeaderElected[{}] failed to renew its lock lease", leaderName, exception);
            return false;
        }
    }

    private void stopRenewal(ScheduledFuture<?> renewal) {
        if (renewal != null) {
            renewal.cancel(false);
        }
    }

    private static ThreadFactory renewalThreadFactory() {
        return task -> {
            Thread thread = new Thread(
                    task,
                    "maritime-leader-lease-renewal-" + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void close() {
        renewalExecutor.shutdownNow();
    }
}
