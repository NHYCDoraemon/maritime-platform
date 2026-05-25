# platform-common-redis

Reusable Redis primitives for Maritime services.

## Idempotency

`IdempotencyPort` supports two compatible usage styles:

- legacy result lookup: `findResult`, `recordResult`, `isProcessed`
- command guard state machine: `beginProcessing`, `completeProcessing`, `failProcessing`, `clearProcessing`

The command guard state machine is domain-neutral:

- `STARTED`: the caller acquired the key and may execute
- `IN_PROGRESS`: another caller owns the in-flight key
- `REPLAY`: a successful result can be reused
- `FAILED`: a terminal failure was recorded
- `CONFLICT`: the same key was reused for a different operation

Consumers own `operationType`, key naming, response serialization, and error mapping.

Default Redis key prefixes are configurable:

- `maritime.redis.idempotency-key-prefix` defaults to `platform:idem`
- `maritime.redis.lock-key-prefix` defaults to `platform:lock`
- `maritime.redis.resilience-key-prefix` defaults to `platform:resilience`

## Resilience

The `resilience` package provides small Redis-backed primitives:

- `SlidingWindowRateLimiter`
- `CircuitBreakerStore`
- `TtlCache`

These APIs are storage and coordination primitives only. Consumers provide keys
and payload serialization at the application boundary.
