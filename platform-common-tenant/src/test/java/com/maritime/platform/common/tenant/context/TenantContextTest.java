package com.maritime.platform.common.tenant.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void set_then_current_returnsValue() {
        TenantContext.set("tenant-1");
        assertThat(TenantContext.current()).isEqualTo("tenant-1");
    }

    @Test
    void clear_after_set_returnsNull() {
        TenantContext.set("tenant-1");
        TenantContext.clear();
        assertThat(TenantContext.current()).isNull();
    }

    @Test
    void each_thread_has_independent_value() throws InterruptedException {
        TenantContext.set("main-tenant");

        AtomicReference<String> threadAValue = new AtomicReference<>();
        AtomicReference<String> threadBValue = new AtomicReference<>();
        CountDownLatch setLatch = new CountDownLatch(2);
        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Thread threadA = new Thread(() -> {
            TenantContext.set("tenant-A");
            setLatch.countDown();
            try {
                readLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threadAValue.set(TenantContext.current());
            doneLatch.countDown();
        });

        Thread threadB = new Thread(() -> {
            TenantContext.set("tenant-B");
            setLatch.countDown();
            try {
                readLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threadBValue.set(TenantContext.current());
            doneLatch.countDown();
        });

        threadA.start();
        threadB.start();

        setLatch.await();
        readLatch.countDown();
        doneLatch.await();

        assertThat(threadAValue.get()).isEqualTo("tenant-A");
        assertThat(threadBValue.get()).isEqualTo("tenant-B");
        assertThat(TenantContext.current()).isEqualTo("main-tenant");
    }

    @Test
    void set_null_current_returnsNull() {
        TenantContext.set(null);
        assertThat(TenantContext.current()).isNull();
    }
}
