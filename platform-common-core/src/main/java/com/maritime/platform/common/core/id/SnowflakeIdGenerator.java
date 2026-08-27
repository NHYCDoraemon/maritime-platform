package com.maritime.platform.common.core.id;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public class SnowflakeIdGenerator {

    public static final long DEFAULT_MAX_CLOCK_BACKWARD_MILLIS = 5_000L;

    private static final long EPOCH = ZonedDateTime.of(
            2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    ).toInstant().toEpochMilli();

    private static final int DATACENTER_ID_BITS = 5;
    private static final int WORKER_ID_BITS = 5;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final int DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long workerId;
    private final long maxClockBackwardMillis;
    private final LongSupplier currentTimeMillis;
    private final LongSupplier monotonicNanos;
    private final LongConsumer pauseNanos;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        this(datacenterId, workerId, DEFAULT_MAX_CLOCK_BACKWARD_MILLIS);
    }

    public SnowflakeIdGenerator(
            long datacenterId,
            long workerId,
            long maxClockBackwardMillis) {
        this(
                datacenterId,
                workerId,
                maxClockBackwardMillis,
                () -> Instant.now().toEpochMilli(),
                System::nanoTime,
                LockSupport::parkNanos
        );
    }

    SnowflakeIdGenerator(
            long datacenterId,
            long workerId,
            long maxClockBackwardMillis,
            LongSupplier currentTimeMillis,
            LongSupplier monotonicNanos,
            LongConsumer pauseNanos) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(
                    "datacenterId must be between 0 and " + MAX_DATACENTER_ID);
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId must be between 0 and " + MAX_WORKER_ID);
        }
        if (maxClockBackwardMillis < 0) {
            throw new IllegalArgumentException("maxClockBackwardMillis must not be negative");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        this.maxClockBackwardMillis = maxClockBackwardMillis;
        this.currentTimeMillis = Objects.requireNonNull(currentTimeMillis, "currentTimeMillis");
        this.monotonicNanos = Objects.requireNonNull(monotonicNanos, "monotonicNanos");
        this.pauseNanos = Objects.requireNonNull(pauseNanos, "pauseNanos");
    }

    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis.getAsLong();

        if (currentTimestamp < lastTimestamp) {
            currentTimestamp = waitForClockRecovery(currentTimestamp);
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis.getAsLong();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis.getAsLong();
        }
        return timestamp;
    }

    private long waitForClockRecovery(long currentTimestamp) {
        long rollbackMillis = lastTimestamp - currentTimestamp;
        if (rollbackMillis > maxClockBackwardMillis) {
            throw clockRollbackFailure(rollbackMillis);
        }

        long startedAtNanos = monotonicNanos.getAsLong();
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(maxClockBackwardMillis);
        while (currentTimestamp < lastTimestamp) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(
                        "Interrupted while waiting for the clock to recover from a "
                                + rollbackMillis + " millisecond rollback");
            }

            long elapsedNanos = monotonicNanos.getAsLong() - startedAtNanos;
            if (elapsedNanos >= timeoutNanos) {
                throw clockRollbackFailure(lastTimestamp - currentTimestamp);
            }

            long remainingNanos = timeoutNanos - elapsedNanos;
            pauseNanos.accept(Math.min(TimeUnit.MILLISECONDS.toNanos(1), remainingNanos));
            currentTimestamp = currentTimeMillis.getAsLong();
        }
        return currentTimestamp;
    }

    private IllegalStateException clockRollbackFailure(long rollbackMillis) {
        return new IllegalStateException(
                "Clock moved backwards. Refusing to generate id for "
                        + rollbackMillis + " milliseconds");
    }
}
