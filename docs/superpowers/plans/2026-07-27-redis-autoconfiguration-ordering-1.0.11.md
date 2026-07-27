# Redis Auto-configuration Ordering and 1.0.11 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every Redis-backed platform auto-configuration run after its Spring Boot prerequisites, then prepare the complete Maven reactor and BOM for the immutable `1.0.11` release.

**Architecture:** Keep the existing conditional Bean contracts and public APIs unchanged. Correct only Spring Boot auto-configuration ordering, prove the real Boot composition with `ApplicationContextRunner`, then update every reactor version reference atomically.

**Tech Stack:** Java 17, Spring Boot 3.3.13 auto-configuration, Spring Data Redis, Jackson, JUnit 5, AssertJ, Mockito, Maven, Testcontainers 1.21.4, Finch 1.17.2.

## Global Constraints

- Preserve `@ConditionalOnClass`, `@ConditionalOnBean`, and `@ConditionalOnMissingBean`; missing optional infrastructure must not fail application startup.
- Do not change Redis algorithms, key formats, timeouts, serialization, Bean names, public types, or `AutoConfiguration.imports`.
- Treat Redis auto-configuration ordering as `ENHANCE_PLATFORM`; do not add any application or domain concept.
- Keep IAM, Todo, and Process repositories read-only in this batch.
- Prepare version `1.0.11`; never overwrite or redeploy `1.0.10`.
- Do not create a Git tag or publish GitHub Packages in this plan.
- Use test-first RED/GREEN for the behavior fix.
- Run focused L2 verification before Finch-backed L3 full-repository verification.

---

### Task 1: Order Redis-dependent auto-configurations

**Files:**
- Create: `platform-common-redis/src/test/java/com/maritime/platform/common/redis/config/RedisDependentAutoConfigurationTest.java`
- Modify: `platform-common-redis/src/main/java/com/maritime/platform/common/redis/lock/DistributedLockAutoConfiguration.java`
- Modify: `platform-common-redis/src/main/java/com/maritime/platform/common/redis/lockport/LockPortAutoConfiguration.java`
- Modify: `platform-common-redis/src/main/java/com/maritime/platform/common/redis/idempotency/IdempotencyAutoConfiguration.java`
- Modify: `platform-common-redis/src/main/java/com/maritime/platform/common/redis/resilience/ResilienceAutoConfiguration.java`
- Modify: `platform-common-redis/src/main/java/com/maritime/platform/common/redis/leader/LeaderElectedAutoConfiguration.java`

**Interfaces:**
- Consumes: Spring Boot `RedisAutoConfiguration`, `JacksonAutoConfiguration`, `RedisConnectionFactory`, and the existing platform Redis auto-configurations.
- Produces: unchanged `DistributedLockAspect`, `LockPort`, `LeaderElectedAspect`, `IdempotencyPort`, `SlidingWindowRateLimiter`, `CircuitBreakerStore`, and `TtlCache` Bean contracts.

- [ ] **Step 1: Write the failing Boot-composition test**

Create `RedisDependentAutoConfigurationTest` with one test that supplies only a
`RedisConnectionFactory` and lets Spring Boot create `StringRedisTemplate` and
`ObjectMapper`:

```java
package com.maritime.platform.common.redis.config;

import com.maritime.platform.common.redis.idempotency.IdempotencyAutoConfiguration;
import com.maritime.platform.common.redis.idempotency.IdempotencyPort;
import com.maritime.platform.common.redis.leader.LeaderElectedAspect;
import com.maritime.platform.common.redis.leader.LeaderElectedAutoConfiguration;
import com.maritime.platform.common.redis.lock.DistributedLockAspect;
import com.maritime.platform.common.redis.lock.DistributedLockAutoConfiguration;
import com.maritime.platform.common.redis.lockport.LockPort;
import com.maritime.platform.common.redis.lockport.LockPortAutoConfiguration;
import com.maritime.platform.common.redis.resilience.CircuitBreakerStore;
import com.maritime.platform.common.redis.resilience.ResilienceAutoConfiguration;
import com.maritime.platform.common.redis.resilience.SlidingWindowRateLimiter;
import com.maritime.platform.common.redis.resilience.TtlCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisDependentAutoConfigurationTest {

    private final ApplicationContextRunner redisRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            DistributedLockAutoConfiguration.class,
                            IdempotencyAutoConfiguration.class,
                            LeaderElectedAutoConfiguration.class,
                            LockPortAutoConfiguration.class,
                            ResilienceAutoConfiguration.class,
                            JacksonAutoConfiguration.class,
                            RedisAutoConfiguration.class))
                    .withBean(RedisConnectionFactory.class,
                            () -> mock(RedisConnectionFactory.class));

    @Test
    void createsRedisBackedBeansAfterBootAutoConfigurations() {
        redisRunner.run(context -> assertThat(context)
                .hasSingleBean(StringRedisTemplate.class)
                .hasSingleBean(DistributedLockAspect.class)
                .hasSingleBean(LockPort.class)
                .hasSingleBean(LeaderElectedAspect.class)
                .hasSingleBean(IdempotencyPort.class)
                .hasSingleBean(SlidingWindowRateLimiter.class)
                .hasSingleBean(CircuitBreakerStore.class)
                .hasSingleBean(TtlCache.class));
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
mvn -B -ntp -pl platform-common-redis -am \
  -Dtest=RedisDependentAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `FAILURE`; Boot creates `StringRedisTemplate`, but the assertion reports
that at least `DistributedLockAspect` is missing because the platform
auto-configurations were evaluated before `RedisAutoConfiguration`.

- [ ] **Step 3: Add the minimal ordering annotations**

In each Redis-backed configuration, import Boot Redis auto-configuration and
replace the unqualified annotation:

```java
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@AutoConfiguration(after = RedisAutoConfiguration.class)
```

Apply that exact change to:

```text
DistributedLockAutoConfiguration
LockPortAutoConfiguration
ResilienceAutoConfiguration
```

For idempotency, order after both prerequisites:

```java
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;

@AutoConfiguration(after = {
        RedisAutoConfiguration.class,
        JacksonAutoConfiguration.class
})
```

For leader election, order after the platform lock port:

```java
@AutoConfiguration(after = LockPortAutoConfiguration.class)
```

No other production lines change.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```bash
mvn -B -ntp -pl platform-common-redis -am \
  -Dtest=RedisDependentAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`; the test creates one Bean for every asserted platform
primitive without connecting to a Redis server.

- [ ] **Step 5: Add missing-dependency and consumer-override contract tests**

Add a shared configuration factory and replace the inline
`AutoConfigurations.of(...)` call:

```java
private static AutoConfigurations redisAutoConfigurations() {
    return AutoConfigurations.of(
            DistributedLockAutoConfiguration.class,
            IdempotencyAutoConfiguration.class,
            LeaderElectedAutoConfiguration.class,
            LockPortAutoConfiguration.class,
            ResilienceAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            RedisAutoConfiguration.class);
}
```

Initialize the main runner with:

```java
private final ApplicationContextRunner redisRunner =
        new ApplicationContextRunner()
                .withConfiguration(redisAutoConfigurations())
                .withBean(RedisConnectionFactory.class,
                        () -> mock(RedisConnectionFactory.class));
```

Add the two contract tests:

```java
@Test
void backsOffCleanlyWithoutRedisConnectionFactory() {
    new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("io.lettuce.core"))
            .withConfiguration(redisAutoConfigurations())
            .run(context -> assertThat(context)
                    .doesNotHaveBean(StringRedisTemplate.class)
                    .doesNotHaveBean(DistributedLockAspect.class)
                    .doesNotHaveBean(LockPort.class)
                    .doesNotHaveBean(LeaderElectedAspect.class)
                    .doesNotHaveBean(IdempotencyPort.class)
                    .doesNotHaveBean(SlidingWindowRateLimiter.class)
                    .doesNotHaveBean(CircuitBreakerStore.class)
                    .doesNotHaveBean(TtlCache.class));
}

@Test
void backsOffForConsumerLockAndIdempotencyPorts() {
    LockPort customLockPort = mock(LockPort.class);
    IdempotencyPort customIdempotencyPort = mock(IdempotencyPort.class);

    redisRunner
            .withBean(LockPort.class, () -> customLockPort)
            .withBean(IdempotencyPort.class, () -> customIdempotencyPort)
            .run(context -> {
                assertThat(context).hasSingleBean(LockPort.class);
                assertThat(context.getBean(LockPort.class))
                        .isSameAs(customLockPort);
                assertThat(context).hasSingleBean(IdempotencyPort.class);
                assertThat(context.getBean(IdempotencyPort.class))
                        .isSameAs(customIdempotencyPort);
                assertThat(context).hasSingleBean(LeaderElectedAspect.class);
            });
}
```

The `FilteredClassLoader` is required because Boot creates a default
`LettuceConnectionFactory` whenever Lettuce is visible, even when the test does
not explicitly register a `RedisConnectionFactory`. Hiding Lettuce models a
consumer without Redis client infrastructure and makes the backoff condition
observable without changing production code.

- [ ] **Step 6: Run the complete Redis module test suite**

Run:

```bash
mvn -B -ntp -pl platform-common-redis -am test
```

Expected: `BUILD SUCCESS`; all existing Redis tests and the three new contract
tests pass.

- [ ] **Step 7: Inspect and commit the behavior fix**

Run:

```bash
git diff --check
git diff -- platform-common-redis
git status --short
```

Verify that only the six Task 1 files changed and no unused import remains.

Commit:

```bash
git add \
  platform-common-redis/src/test/java/com/maritime/platform/common/redis/config/RedisDependentAutoConfigurationTest.java \
  platform-common-redis/src/main/java/com/maritime/platform/common/redis/lock/DistributedLockAutoConfiguration.java \
  platform-common-redis/src/main/java/com/maritime/platform/common/redis/lockport/LockPortAutoConfiguration.java \
  platform-common-redis/src/main/java/com/maritime/platform/common/redis/idempotency/IdempotencyAutoConfiguration.java \
  platform-common-redis/src/main/java/com/maritime/platform/common/redis/resilience/ResilienceAutoConfiguration.java \
  platform-common-redis/src/main/java/com/maritime/platform/common/redis/leader/LeaderElectedAutoConfiguration.java
git commit -m "fix(redis): order dependent auto-configurations"
```

---

### Task 2: Prepare reactor version 1.0.11 and complete L3 verification

**Files:**
- Modify: `pom.xml`
- Modify: `platform-bom/pom.xml`
- Modify: `platform-common-core/pom.xml`
- Modify: `platform-common-web/pom.xml`
- Modify: `platform-common-security/pom.xml`
- Modify: `platform-common-mybatis/pom.xml`
- Modify: `platform-common-redis/pom.xml`
- Modify: `platform-common-mq/pom.xml`
- Modify: `platform-common-outbox/pom.xml`
- Modify: `platform-common-notification/pom.xml`
- Modify: `platform-common-tenant/pom.xml`
- Modify: `platform-common-metrics/pom.xml`
- Modify: `platform-common-feign/pom.xml`
- Modify: `platform-common-openapi/pom.xml`
- Modify: `iam-sdk/pom.xml`
- Modify: `platform-gateway-starter/pom.xml`
- Modify: `README.md`

**Interfaces:**
- Consumes: the verified Task 1 source tree and Maven reactor module graph.
- Produces: a consistent `com.maritime.platform:*:1.0.11` reactor and
  `platform-bom:1.0.11`, without publishing or tagging.

- [ ] **Step 1: Change every reactor version reference atomically**

In the root POM:

```xml
<groupId>com.maritime.platform</groupId>
<artifactId>maritime-platform</artifactId>
<version>1.0.11</version>
```

In `platform-bom/pom.xml`:

```xml
<artifactId>platform-bom</artifactId>
<version>1.0.11</version>
...
<platform.version>1.0.11</platform.version>
```

In every other module POM listed under Task 2, update only its parent version:

```xml
<parent>
    <groupId>com.maritime.platform</groupId>
    <artifactId>maritime-platform</artifactId>
    <version>1.0.11</version>
</parent>
```

In the README BOM example:

```xml
<artifactId>platform-bom</artifactId>
<version>1.0.11</version>
```

- [ ] **Step 2: Prove there are no stale release coordinates**

Run:

```bash
rg -n '1\.0\.10' --glob 'pom.xml' README.md
```

Expected: no output and exit code `1`, meaning no stale `1.0.10` occurrence
remains in POM files or the README.

Run:

```bash
mvn -q help:evaluate -Dexpression=project.version -DforceStdout
mvn -q -pl platform-bom help:evaluate \
  -Dexpression=project.version -DforceStdout
```

Expected: each command prints `1.0.11`.

- [ ] **Step 3: Run Finch-backed full repository tests**

Confirm Finch is running and pre-pull Ryuk:

```bash
finch version
finch pull testcontainers/ryuk:0.12.0
```

Run the full suite with the Finch socket and Testcontainers compatibility
settings:

```bash
DOCKER_HOST=unix:///Applications/Finch/lima/data/finch/sock/finch.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
TESTCONTAINERS_CHECKS_DISABLE=true \
mvn -B -ntp -Dapi.version=1.43 test
```

Expected: all reactor modules report `SUCCESS`, with zero Surefire failures and
errors.

Aggregate the reports:

```bash
rg -uu -o \
  'tests="[0-9]+"|failures="[0-9]+"|errors="[0-9]+"|skipped="[0-9]+"' \
  -g 'TEST-*.xml' . \
  | sed 's/.*://' \
  | sed 's/="/ /; s/"//' \
  | awk '{sum[$1]+=$2} END {
      printf "tests=%d failures=%d errors=%d skipped=%d\n",
             sum["tests"], sum["failures"], sum["errors"], sum["skipped"]
    }'
```

Expected: `failures=0 errors=0`. Record the actual test and skipped counts
rather than copying an older run.

- [ ] **Step 4: Verify release packaging**

Run:

```bash
mvn -B -ntp -DskipTests package
```

Expected: all reactor modules report `SUCCESS`; Maven resolves every sibling
artifact at `1.0.11`.

- [ ] **Step 5: Review the complete branch and commit release preparation**

Run:

```bash
git diff --check
git diff --stat main...HEAD
git diff main...HEAD -- \
  pom.xml '*/pom.xml' README.md platform-common-redis
git status --short
```

Verify:

- public Java interfaces and Bean names are unchanged;
- only ordering metadata changed in production Java;
- all POM coordinates are `1.0.11`;
- README uses `1.0.11`;
- no consumer repository file, tag, or publish workflow changed.

Commit:

```bash
git add pom.xml README.md \
  platform-bom/pom.xml \
  platform-common-core/pom.xml \
  platform-common-web/pom.xml \
  platform-common-security/pom.xml \
  platform-common-mybatis/pom.xml \
  platform-common-redis/pom.xml \
  platform-common-mq/pom.xml \
  platform-common-outbox/pom.xml \
  platform-common-notification/pom.xml \
  platform-common-tenant/pom.xml \
  platform-common-metrics/pom.xml \
  platform-common-feign/pom.xml \
  platform-common-openapi/pom.xml \
  iam-sdk/pom.xml \
  platform-gateway-starter/pom.xml
git commit -m "build(release): prepare platform 1.0.11"
```

- [ ] **Step 6: Capture completion evidence before integration**

Run:

```bash
git status --short --branch
git log -3 --oneline --decorate
git diff --check main...HEAD
```

Record in the completion summary:

- platform ownership: `ENHANCE_PLATFORM`;
- focused RED failure and GREEN result;
- Redis module test result;
- Finch full-suite test count and zero-failure count;
- package result;
- changed modules;
- no L4 claim, because this batch verifies local Finch infrastructure rather
  than a deployed production environment;
- residual consumer action: upgrade to `platform-bom:1.0.11`, verify, then
  remove process-engine compatibility configurations in a separate change.

Do not push, tag, publish, or merge until the branch-finishing choice is made.
