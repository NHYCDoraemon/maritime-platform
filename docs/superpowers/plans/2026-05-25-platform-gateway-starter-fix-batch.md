# Platform Gateway Starter Fix Batch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `platform-gateway-starter` 审计发现的 4 个阻断问题，让 starter 真正满足“新项目引入依赖 + 配置即可安全复用 gateway”的目标。

**Architecture:** 本批只做定向修复，不扩大 starter 能力边界。修复策略是先补失败回归测试，再修改最小生产代码，并用 `mvn -pl platform-gateway-starter -am test` 验证完整 starter。

**Tech Stack:** Java 17, Spring Cloud Gateway, Spring Boot AutoConfiguration, Reactor, Reactive Redis, JUnit 5, AssertJ, Testcontainers Redis.

---

## 平台能力归属判定

| 能力 | 判定 | 是否可进平台 | 依据 | 兼容或替换策略 |
|---|---|---:|---|---|
| HMAC 入站签名 header 与下游可信上下文 header 分离 | ENHANCE_PLATFORM | 是 | HMAC 是系统调用横向安全链，必须在 starter 内保证入站签名可验证且下游不可伪造上下文 | 修复过滤链，不改业务调用模型 |
| JWT session/blacklist/user-enabled 自动接入 | ENHANCE_PLATFORM | 是 | 用户请求标准安全链已写入 starter 目标，必须在默认自动装配路径生效 | 修复构造器注入，保持配置开关兼容 |
| route auth-mode fail-closed 校验 | ENHANCE_PLATFORM | 是 | 配置驱动认证必须避免漏配导致静默放行 | 启动期拒绝错误配置 |
| HMAC timestamp 契约一致性 | ENHANCE_PLATFORM | 是 | HMAC 客户端和 gateway 必须共享稳定签名契约 | 统一为 epoch millis，更新文档和测试 |

平台版本影响：不新增模块，不调整 BOM 结构，只修复 `platform-gateway-starter` 行为和文档。

消费者影响：修复后新项目按文档接入即可；已按错误 README 使用秒级 timestamp 的调用方需要改为毫秒级 timestamp。

风险：如果只改单元组件测试而不覆盖自动装配和完整过滤链，问题会再次遗漏。

## Task 1: 修复 HMAC 入站签名 header 被可信头清理破坏

**Files:**
- Modify: `platform-gateway-starter/src/main/java/com/maritime/platform/gateway/filter/UntrustedHeaderStripFilter.java`
- Modify: `platform-gateway-starter/src/main/java/com/maritime/platform/gateway/filter/HmacAuthenticationGatewayFilter.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/filter/GatewayFilterChainTest.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/filter/HmacAuthenticationGatewayFilterTest.java`

- [ ] **Step 1: Write failing full-chain regression**

Add a test proving that `X-App-Key`, `X-Timestamp`, `X-Nonce`, `X-Body-Digest`, and `X-Signature` survive `UntrustedHeaderStripFilter` long enough for HMAC authentication.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest#hmacSigningHeadersSurvivePreAuthStrip test`

Expected: FAIL because `X-App-Key` is removed before HMAC authentication.

- [ ] **Step 3: Fix header classification**

Remove inbound HMAC signing headers from the pre-auth trusted context cleanup list. Keep downstream context headers such as `X-App-Code`, `X-App-Id`, `X-Verified-App-Code`, `X-App-Permissions`, tenant and user headers in the cleanup list.

- [ ] **Step 4: Strip raw HMAC signing headers after successful HMAC auth**

In `HmacAuthenticationGatewayFilter`, after successful authentication and before forwarding, mutate the cached request to remove configured HMAC signing headers so downstream services only receive verified context headers injected by `ContextHeaderInjectionFilter`.

- [ ] **Step 5: Verify**

Run: `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add platform-gateway-starter/src/main/java/com/maritime/platform/gateway/filter/UntrustedHeaderStripFilter.java \
        platform-gateway-starter/src/main/java/com/maritime/platform/gateway/filter/HmacAuthenticationGatewayFilter.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/filter/GatewayFilterChainTest.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/filter/HmacAuthenticationGatewayFilterTest.java
git commit -m "fix(gateway): preserve hmac signing headers before auth"
```

## Task 2: 修复 JWT 状态校验未接入自动装配认证链

**Files:**
- Modify: `platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/jwt/JwtAuthenticationManager.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/integration/GatewayStarterIntegrationTest.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/jwt/JwtAuthenticationManagerTest.java`

- [ ] **Step 1: Write failing auto-wiring regression**

Add an integration test that starts the real starter context with `jwt.enabled=true`, obtains `JwtAuthenticationManager`, authenticates a valid signed JWT whose session key is absent in Redis, and expects `SESSION_EXPIRED`.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `mvn -pl platform-gateway-starter -Dtest=GatewayStarterIntegrationTest#jwtAuthenticationManagerUsesStateValidatorWhenAutoWired test`

Expected: FAIL because the auto-wired `JwtAuthenticationManager` currently has `stateValidator == null`.

- [ ] **Step 3: Fix constructor injection**

Change the Spring constructor so `JwtStateValidator` is injected when present:

```java
@Autowired
public JwtAuthenticationManager(
        GatewaySecurityProperties properties,
        JwtClaimsMapper claimsMapper,
        JwtStateValidator stateValidator) {
    this(properties, claimsMapper, Clock.systemUTC(), stateValidator);
}
```

If the implementation needs optional validation for tests, keep a package-private constructor for tests but make the Spring path strict when JWT validation is enabled.

- [ ] **Step 4: Add positive and negative state-path checks**

Cover at least:

- valid token + missing session -> `SESSION_EXPIRED`
- valid token + present session -> principal returned
- blacklisted `jti` -> `TOKEN_BLACKLISTED`
- disabled user -> `USER_DISABLED`

- [ ] **Step 5: Verify**

Run: `mvn -pl platform-gateway-starter -Dtest=GatewayStarterIntegrationTest,JwtAuthenticationManagerTest test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/jwt/JwtAuthenticationManager.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/integration/GatewayStarterIntegrationTest.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/jwt/JwtAuthenticationManagerTest.java
git commit -m "fix(gateway): wire jwt state validation into auth manager"
```

## Task 3: 修复 route auth-mode 缺失导致静默放行

**Files:**
- Modify: `platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/GatewaySecurityProperties.java`
- Modify: `platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/RouteSecurityPolicyResolver.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/GatewaySecurityPropertiesTest.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/RouteSecurityPolicyResolverTest.java`

- [ ] **Step 1: Write failing validation regression**

Add a test where a route has `id` and `paths` but no `auth-mode`. The context or `afterPropertiesSet()` must fail instead of producing a null `RouteSecurityPolicy`.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,RouteSecurityPolicyResolverTest test`

Expected: FAIL because missing `auth-mode` is currently accepted.

- [ ] **Step 3: Fail closed in configuration**

Add `@NotNull` to `GatewaySecurityProperties.RoutePolicy.authMode` and add explicit startup validation:

```java
if (rp.authMode == null) {
    throw new IllegalStateException("Route '" + rp.id + "' must configure auth-mode");
}
```

- [ ] **Step 4: Fail closed in programmatic registration**

Reject `addRoutePolicy(..., null)` in `RouteSecurityPolicyResolver` with a clear `IllegalArgumentException`.

- [ ] **Step 5: Verify**

Run: `mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,RouteSecurityPolicyResolverTest test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/GatewaySecurityProperties.java \
        platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/RouteSecurityPolicyResolver.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/GatewaySecurityPropertiesTest.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/RouteSecurityPolicyResolverTest.java
git commit -m "fix(gateway): reject route policies without auth mode"
```

## Task 4: 修复 HMAC timestamp 契约不一致

**Files:**
- Modify: `platform-gateway-starter/README.md`
- Modify: `platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/hmac/HmacAuthenticationManager.java`
- Test: `platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/hmac/HmacAuthenticationManagerTest.java`

- [ ] **Step 1: Lock timestamp unit with tests**

Add tests proving epoch millis is accepted and epoch seconds is rejected with `TIMESTAMP_EXPIRED` under the default tolerance.

- [ ] **Step 2: Run focused tests**

Run: `mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest test`

Expected: PASS after current behavior is explicitly covered.

- [ ] **Step 3: Fix public documentation**

Change README HMAC request standard from “Unix 时间戳（秒）” to “Unix epoch 毫秒时间戳”。Keep the code contract aligned with existing `HmacAuthenticationManager` and `platform-common-core` HMAC utility, both of which use millis.

- [ ] **Step 4: Verify**

Run: `mvn -pl platform-gateway-starter -am test`

Expected: PASS with no test failures.

- [ ] **Step 5: Commit**

```bash
git add platform-gateway-starter/README.md \
        platform-gateway-starter/src/main/java/com/maritime/platform/gateway/security/hmac/HmacAuthenticationManager.java \
        platform-gateway-starter/src/test/java/com/maritime/platform/gateway/security/hmac/HmacAuthenticationManagerTest.java
git commit -m "docs(gateway): align hmac timestamp contract"
```

## Final Verification

- [ ] Run `mvn -pl platform-gateway-starter -am test`.
- [ ] Confirm HMAC full-chain request can authenticate with `X-App-Key` present before auth.
- [ ] Confirm JWT manager auto-wired from Spring enforces session, blacklist, and user-enabled checks.
- [ ] Confirm route policies without auth-mode fail at startup.
- [ ] Confirm README and tests both document epoch millis for HMAC timestamp.
