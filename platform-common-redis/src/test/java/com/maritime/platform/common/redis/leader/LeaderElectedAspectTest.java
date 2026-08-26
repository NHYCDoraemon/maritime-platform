package com.maritime.platform.common.redis.leader;

import com.maritime.platform.common.redis.lockport.LockPort;
import com.maritime.platform.common.redis.lockport.LockPort.LockHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LeaderElectedAspect}. Uses Mockito to stub {@link LockPort}
 * — no Redis required.
 */
class LeaderElectedAspectTest {

    private LockPort lockPort;
    private LeaderElectedAspect aspect;

    @BeforeEach
    void setUp() {
        lockPort = mock(LockPort.class);
        aspect = new LeaderElectedAspect(lockPort);
    }

    @AfterEach
    void tearDown() {
        aspect.close();
    }

    static class ScanService {
        final AtomicInteger voidCalls = new AtomicInteger();
        final AtomicInteger returningCalls = new AtomicInteger();
        final CountDownLatch renewalObserved = new CountDownLatch(1);

        @LeaderElected(name = "scan-job")
        public void runVoid() {
            voidCalls.incrementAndGet();
        }

        @LeaderElected(name = "lookup-job", silentSkip = false)
        public String lookup() {
            returningCalls.incrementAndGet();
            return "result";
        }

        @LeaderElected(name = "configurable", waitMillis = 1000, leaseMillis = 5000)
        public void runConfigurable() {
            voidCalls.incrementAndGet();
        }

        @LeaderElected(name = "renewable", leaseMillis = 60, renewLease = true)
        public void runRenewable() {
            voidCalls.incrementAndGet();
        }

        @LeaderElected(name = "slow-renewable", leaseMillis = 60, renewLease = true)
        public void runUntilRenewed() {
            try {
                if (!renewalObserved.await(1, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("scheduled renewal did not run");
                }
                voidCalls.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for renewal", exception);
            }
        }

        @LeaderElected(name = "invalid-renewable", leaseMillis = 0, renewLease = true)
        public void runWithInvalidLease() {
            voidCalls.incrementAndGet();
        }
    }

    private ScanService proxyOf(ScanService target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    private LockHandle fakeHandle() {
        return new LockHandle() {
            @Override public String lockKey() { return "platform:lock:leader:x"; }
            @Override public void unlock() { }
            @Override public void close() { }
        };
    }

    @Test
    void voidMethod_whenAcquiresLock_executesBody() {
        when(lockPort.tryLock(eq("leader"), eq("scan-job"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(fakeHandle()));

        ScanService target = new ScanService();
        proxyOf(target).runVoid();

        assertThat(target.voidCalls.get()).isEqualTo(1);
        verify(lockPort, times(1)).tryLock(eq("leader"), eq("scan-job"),
                any(Duration.class), any(Duration.class));
    }

    @Test
    void voidMethod_whenLockHeld_skipsBody() {
        when(lockPort.tryLock(eq("leader"), eq("scan-job"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.empty());

        ScanService target = new ScanService();
        proxyOf(target).runVoid();

        assertThat(target.voidCalls.get()).isZero();
    }

    @Test
    void methodWithReturn_whenSkipped_returnsNull() {
        when(lockPort.tryLock(eq("leader"), eq("lookup-job"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.empty());

        ScanService target = new ScanService();
        String result = proxyOf(target).lookup();

        assertThat(result).isNull();
        assertThat(target.returningCalls.get()).isZero();
    }

    @Test
    void methodWithReturn_whenAcquired_returnsResult() {
        when(lockPort.tryLock(eq("leader"), eq("lookup-job"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(fakeHandle()));

        ScanService target = new ScanService();
        String result = proxyOf(target).lookup();

        assertThat(result).isEqualTo("result");
        assertThat(target.returningCalls.get()).isEqualTo(1);
    }

    @Test
    void annotationAttributes_arePassedToLockPort() {
        when(lockPort.tryLock(eq("leader"), eq("configurable"),
                eq(Duration.ofMillis(1000)), eq(Duration.ofMillis(5000))))
                .thenReturn(Optional.of(fakeHandle()));

        ScanService target = new ScanService();
        proxyOf(target).runConfigurable();

        assertThat(target.voidCalls.get()).isEqualTo(1);
        verify(lockPort, times(1)).tryLock(eq("leader"), eq("configurable"),
                eq(Duration.ofMillis(1000)), eq(Duration.ofMillis(5000)));
    }

    @Test
    void lockReleased_viaHandleClose() {
        LockHandle handle = mock(LockHandle.class);
        when(lockPort.tryLock(eq("leader"), eq("scan-job"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(handle));

        proxyOf(new ScanService()).runVoid();

        verify(handle, times(1)).close();
        verify(handle, never()).unlock();
    }

    @Test
    void renewalEnabled_renewsOwnedLease() {
        LockHandle handle = mock(LockHandle.class);
        when(handle.renew(Duration.ofMillis(60))).thenReturn(true);
        when(lockPort.tryLock(eq("leader"), eq("renewable"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(handle));

        ScanService target = new ScanService();
        proxyOf(target).runRenewable();

        assertThat(target.voidCalls.get()).isEqualTo(1);
        verify(handle, atLeastOnce()).renew(Duration.ofMillis(60));
        verify(handle).close();
    }

    @Test
    void renewalEnabled_whenOwnershipLost_failsInvocation() {
        LockHandle handle = mock(LockHandle.class);
        when(handle.renew(Duration.ofMillis(60))).thenReturn(false);
        when(lockPort.tryLock(eq("leader"), eq("renewable"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(handle));

        ScanService target = new ScanService();

        assertThatThrownBy(() -> proxyOf(target).runRenewable())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("renewable")
                .hasMessageContaining("lost lock ownership");
        verify(handle).close();
    }

    @Test
    void renewalEnabled_renewsPeriodicallyBeforeMethodReturns() {
        LockHandle handle = mock(LockHandle.class);
        ScanService target = new ScanService();
        when(handle.renew(Duration.ofMillis(60))).thenAnswer(invocation -> {
            target.renewalObserved.countDown();
            return true;
        });
        when(lockPort.tryLock(
                eq("leader"), eq("slow-renewable"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(handle));

        proxyOf(target).runUntilRenewed();

        assertThat(target.voidCalls.get()).isEqualTo(1);
        verify(handle, org.mockito.Mockito.atLeast(2)).renew(Duration.ofMillis(60));
        verify(handle).close();
    }

    @Test
    void renewalEnabled_withInvalidLease_releasesAcquiredLock() {
        LockHandle handle = mock(LockHandle.class);
        when(lockPort.tryLock(
                eq("leader"), eq("invalid-renewable"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(handle));

        ScanService target = new ScanService();

        assertThatThrownBy(() -> proxyOf(target).runWithInvalidLease())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leaseMillis must be positive");
        assertThat(target.voidCalls.get()).isZero();
        verify(handle).close();
    }
}
