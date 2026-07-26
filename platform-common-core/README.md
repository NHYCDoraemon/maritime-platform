# platform-common-core

## Snowflake ID generator

`SnowflakeIdGenerator` keeps the existing `(datacenterId, workerId)` constructor
and ID bit layout. Small wall-clock rollbacks are now handled by waiting for the
clock to catch up; a larger or non-recovering rollback still fails closed before
an ID can be emitted.

Spring Boot configuration:

```yaml
iam:
  snowflake:
    datacenter-id: 0
    worker-id: 0
    max-clock-backward-millis: 5000
```

Each concurrently running application replica must have a distinct
`(datacenter-id, worker-id)` pair. The rollback tolerance does not replace that
deployment invariant.
