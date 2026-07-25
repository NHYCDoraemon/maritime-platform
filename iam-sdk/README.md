# IAM SDK

`iam-sdk` is the shared integration layer between Maritime business
applications and IAM. IAM remains the source of truth for effective
permissions; consumers use this module for identity context, permission
enforcement, data-scope injection, resource publication, and cache
reconciliation.

## Reactive gateway integration

Spring Cloud Gateway applications receive a
`ReactivePermissionCodeProvider` when WebFlux, reactive Redis, and
`iam.center.url` are present.

```yaml
iam:
  center:
    url: http://iam-query:9083
  app:
    code: TODO
  sdk:
    fail-open: false
    cache:
      permission-codes-ttl: 30m
      empty-permission-codes-ttl: 2m
      version-check-interval: 1s
      max-permission-header-bytes: 16384
```

```java
return permissionCodeProvider
        .getPermissionCodes(userId, activeOrgCode)
        .map(snapshot -> exchange.mutate()
                .request(request -> request.headers(headers ->
                        headers.set(
                                "X-App-Permissions",
                                snapshot.headerValue())))
                .build());
```

The provider:

- reconciles a short-lived local version observation with IAM's
  authoritative permission version;
- stores versioned permission codes in Redis and negative-caches an empty
  grant set for a shorter period;
- coalesces concurrent refreshes for the same identity;
- bypasses a failed Redis cache and queries IAM directly;
- rejects oversized trusted permission headers;
- fails closed by default;
- can read a cached stale grant only when `iam.sdk.fail-open=true` is
  explicitly configured;
- falls back to TTL-only behavior when connected to an older IAM that does
  not yet expose the version endpoint.

RabbitMQ invalidation is the low-latency path. Version reconciliation is the
correctness path when an event is delayed, lost, or published on a different
broker/vhost. Applications must not depend on MQ delivery alone.

## Cache contract

Permission-code cache keys retain the established form:

```text
iam:perms:{systemCode}:{userId}:{activeOrgCode}
```

The stored value is versioned:

```text
ver:{globalVersion}.{systemVersion}|code1,code2
```

IAM permission-change events can invalidate one user, one system, or all
systems. Empty `userIds` means system-wide invalidation.

## Compatibility and security

- Servlet auto-configuration remains separate from reactive
  auto-configuration.
- Consumers running against an older IAM continue to work with TTL-only
  reconciliation.
- An unavailable IAM produces `IamPermissionUnavailableException` unless
  fail-open was deliberately enabled and a cached value exists.
- The gateway must strip client-supplied identity and permission headers
  before adding trusted values.
- A business service must still enforce its own permission annotations; UI
  visibility is not an authorization boundary.
