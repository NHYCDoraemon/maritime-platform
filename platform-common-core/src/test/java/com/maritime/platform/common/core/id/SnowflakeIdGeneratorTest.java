package com.maritime.platform.common.core.id;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {

    @Test
    void nextId_smallClockRollback_waitsForRecoveryAndKeepsIdsUnique() {
        long[] wallClock = {1_000L, 999L, 1_000L};
        AtomicInteger wallClockIndex = new AtomicInteger();
        LongSupplier wallClockSource =
                () -> wallClock[Math.min(wallClockIndex.getAndIncrement(), wallClock.length - 1)];
        AtomicLong monotonicNanos = new AtomicLong();
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1,
                2,
                5,
                wallClockSource,
                () -> monotonicNanos.getAndAdd(1_000_000L),
                ignored -> {
                }
        );

        long first = generator.nextId();
        long second = generator.nextId();

        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void nextId_clockRollbackBeyondTolerance_failsClosed() {
        long[] wallClock = {1_000L, 994L};
        AtomicInteger wallClockIndex = new AtomicInteger();
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1,
                2,
                5,
                () -> wallClock[Math.min(wallClockIndex.getAndIncrement(), wallClock.length - 1)],
                System::nanoTime,
                ignored -> {
                }
        );

        generator.nextId();

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("6 milliseconds");
    }

    @Test
    void nextId_clockDoesNotRecoverWithinTolerance_failsClosed() {
        long[] wallClock = {1_000L, 999L};
        AtomicInteger wallClockIndex = new AtomicInteger();
        AtomicLong monotonicNanos = new AtomicLong();
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1,
                2,
                5,
                () -> wallClock[Math.min(wallClockIndex.getAndIncrement(), wallClock.length - 1)],
                () -> monotonicNanos.getAndAdd(3_000_000L),
                ignored -> {
                }
        );

        generator.nextId();

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Clock moved backwards");
    }

    @Test
    void constructor_negativeRollbackTolerance_rejected() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1, 2, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxClockBackwardMillis");
    }
}
