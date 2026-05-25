# Platform Gateway Starter Audit Hardening Batch

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` task-by-task. Each task must start with a failing regression test, then make the smallest production change needed to pass.

**Goal:** 修复 2026-05-26 完整审计发现的 gateway starter 剩余缺口，使 `platform-gateway-starter` 真正满足“新项目只引入依赖和配置即可安全复用 gateway”的目标。

**Current audit result:** 未完全达成目标。现有 `mvn -pl platform-gateway-starter -am test` 通过，但测试没有覆盖若干 fail-open、安全契约和运行时集成问题。

**Architecture:** 本批仍只处理平台横向能力，不加入 todo、IAM 管理端、process 等业务语义。修复重点是 fail-closed 配置、可信头生命周期、HMAC 防重放时序、TraceId 透传、公开签名契约和可选 Sentinel 集成运行时行为。

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Cloud Gateway, Reactor, Reactive Redis, JUnit 5, AssertJ, Testcontainers Redis.

---

## 平台能力归属判定

| 能力 | 判定 | 是否可进平台 | 依据 | 兼容或替换策略 |
|---|---|---:|---|---|
| auth-mode 与 jwt/hmac enabled 配置一致性 | ENHANCE_PLATFORM | 是 | route 级认证策略是 gateway starter 的核心横向安全能力，必须 fail closed | 启动期校验；默认开放路径需显式 `default-auth-mode=none` |
| 可配置 HMAC 签名 header 生命周期 | ENHANCE_PLATFORM | 是 | HMAC header 名称已暴露为配置，必须保证认证前可读、认证后不透传 | 属性驱动清理；新增回归测试覆盖 `X-App-Code` 兼容配置 |
| 可信 header 清理完整性与 TraceId 透传 | ENHANCE_PLATFORM | 是 | 下游只能信任 gateway 注入的上下文，同时需要沿用 gateway traceId | 补齐清理清单；认证后重新注入 sanitized `X-Trace-Id` |
| HMAC nonce 提交时序 | ENHANCE_PLATFORM | 是 | 防重放不能允许未认证请求消耗有效 nonce 造成拒绝服务 | 验签和凭证校验通过后再 SETNX |
| HMAC canonical string 公开契约 | ENHANCE_PLATFORM | 是 | 系统调用方必须能按文档稳定生成签名 | 以设计稿为准，代码、README、测试一致；历史 helper 兼容另行显式处理 |
| Sentinel Gateway block 响应 | ENHANCE_PLATFORM | 是 | 可选网关限流集成是横向能力，当前 block handler 运行时会因 null map 失败 | 替换 null map 构造并增加 handler 执行测试 |

平台版本影响：不新增模块；可能需要在 `platform-common-core` 或 `platform-common-security` 增加 additive HMAC helper 时同步 BOM 管理。

消费者影响：修复后错误配置会更早启动失败；如果消费者依赖旧的未命名 HMAC canonical string，需要按 README 迁移。未发布前只影响本 starter 测试契约。

风险：这些问题多为现有测试未覆盖的安全边界。不要只改文档或只改单元组件，必须覆盖自动装配、完整过滤链和运行时 handler。

---

## Task 1: auth-mode 与启用开关 fail-closed

目标：防止 route/default 解析为 `JWT`、`HMAC`、`JWT_OR_HMAC`，但对应认证过滤器因 `jwt.enabled=false` 或 `hmac.enabled=false` 未注册，最终静默放行。

开发内容：

- 在 `GatewaySecurityProperties.afterPropertiesSet()` 中校验 default auth mode 与 route auth mode。
- `JWT` 需要 `jwt.enabled=true`。
- `HMAC` 需要 `hmac.enabled=true`。
- `JWT_OR_HMAC` 需要 `jwt.enabled=true` 且 `hmac.enabled=true`。
- `NONE` 不需要认证组件。
- `JWT_AND_HMAC` 继续作为保留能力启动期拒绝。
- 更新默认配置测试：真正“无认证 gateway”必须显式设置 `default-auth-mode=none`。

验收标准：

- 默认 `default-auth-mode=jwt` 但未启用 JWT 时启动失败。
- route 配置 `auth-mode=hmac` 但未启用 HMAC 时启动失败。
- route 配置 `auth-mode=jwt-or-hmac` 但只启用 JWT 或只启用 HMAC 时启动失败。
- 显式 `default-auth-mode=none` 可启动。

验证命令：

- `mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test`
- `mvn -pl platform-gateway-starter -am test`

## Task 2: HMAC 签名 header 生命周期与可配置 header 支持

目标：让配置化 HMAC 签名 header 在认证前不会被可信头清理破坏，认证决策后也不会作为原始客户端 header 透传到下游。

开发内容：

- 将 `UntrustedHeaderStripFilter` 从静态清单改为属性感知：保留当前配置的 HMAC 签名 header，继续清理用户、租户、应用上下文 header。
- 覆盖 `maritime.gateway.security.hmac.headers.app-key=X-App-Code` 的兼容测试，确保签名 header 不会因与下游上下文 header 同名而在认证前被清理。
- 在 HMAC 认证成功、`JWT_OR_HMAC` 走 JWT 分支、`JWT`/`NONE` 旁路分支中，转发前移除当前配置的原始 HMAC 签名 header。
- README 清楚区分“入站签名 header”和“下游可信上下文 header”。

验收标准：

- 默认 `X-App-Key` 和自定义 `X-App-Code` 都能作为 HMAC app-key header 完成认证。
- 任何转发到下游的请求都不携带原始 `X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature` 或对应自定义签名 header。
- HMAC 认证成功后，下游只收到 `ContextHeaderInjectionFilter` 注入的应用上下文 header。

验证命令：

- `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest,GatewayStarterIntegrationTest test`
- `mvn -pl platform-gateway-starter -am test`

## Task 3: 补齐可信 header 清理并恢复 TraceId 下游透传

目标：达到设计稿的可信头清理目标，同时让下游服务收到 gateway 确认后的 `X-Trace-Id`。

开发内容：

- 在可信头清理清单补齐设计稿中的 `X-User-Permissions`、`X-Test-Channel`。
- 保持客户端原始 `X-Trace-Id` 先被捕获和规范化，再清理客户端 header，随后向下游请求写入 gateway 确认后的 `X-Trace-Id`。
- 确认 public path、JWT path、HMAC path 都执行同样的清理和 trace 透传。
- 更新 README 清理清单，避免继续声称 `X-Trace-Id` 被永久移除。

验收标准：

- 客户端伪造的 `X-User-Permissions`、`X-Test-Channel` 不会到达下游。
- 下游请求始终携带 sanitized `X-Trace-Id`。
- 响应仍携带同一个 `X-Trace-Id`。
- 下游 `platform-common-web` 可以复用 gateway traceId，而不是重新生成。

验证命令：

- `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,TrustedHeaderWriterTest test`
- `mvn -pl platform-gateway-starter -am test`

## Task 4: HMAC nonce 只在验签通过后提交

目标：防止未认证或签名错误请求提前消耗 Redis nonce，造成有效请求被误判 replay。

开发内容：

- 调整 `HmacAuthenticationManager` 顺序：timestamp、nonce 长度、bodyDigest 校验后，先解析 credential，再计算并比对签名，最后执行 Redis SETNX nonce。
- 对 `UNKNOWN_APP`、`APP_DISABLED`、`INVALID_SIGNATURE`、body digest 不匹配等失败路径增加测试，证明不会调用 `nonceValidator.validate()`。
- 对 credential 缺少 `appSecret` 的 Redis 数据 fail closed，返回明确认证错误，不抛出 NPE/500。
- 保持两个相同有效签名请求只有一个通过。

验收标准：

- 无效签名不会写入 nonce。
- unknown/disabled app 不会写入 nonce。
- credential 缺少 secret 不会导致未处理异常。
- 有效请求仍通过 SETNX 防重放。

验证命令：

- `mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest,DefaultAppCredentialResolverTest,HmacNonceValidatorTest test`
- `mvn -pl platform-gateway-starter -am test`

## Task 5: 统一 HMAC canonical string 与公开签名契约

目标：让设计稿、README、代码和测试对系统调用签名算法给出同一个稳定契约。

开发内容：

- 以设计稿中包含 method/path/query 的强 canonical string 为准，并使用带字段名的 7 行格式：

```text
appKey={appKey}
method={HTTP_METHOD}
path={rawPath}
query={canonicalQuery}
timestamp={timestamp}
nonce={nonce}
bodyDigest={sha256Hex(body)}
```

- 更新 `HmacCanonicalRequestBuilder` 和测试，避免当前无字段名 7 行格式与设计稿不一致。
- README 写出完整 canonical string、query canonicalization、bodyDigest、timestamp millis、signature hex 规则。
- 明确 `platform-common-core` 现有 `HmacSignatureValidator` 的旧 `systemCode=...&timestamp=...` helper 与 gateway starter v2 签名契约不兼容；如要兼容历史调用，必须新增显式 `legacy` 模式或 additive helper，不能静默混用。
- 根 README 模块表补充 `platform-gateway-starter`。

验收标准：

- 一个只按 README 生成签名的测试客户端可以通过 `HmacAuthenticationManager`。
- 当前设计稿、starter README 和测试中的 canonical string 完全一致。
- 历史 helper 兼容策略被明确记录，不再靠隐式字段名配置猜测。

验证命令：

- `mvn -pl platform-gateway-starter -Dtest=HmacCanonicalRequestBuilderTest,HmacAuthenticationManagerTest test`
- `mvn -pl platform-gateway-starter -am test`

## Task 6: 修复 Sentinel block handler 运行时响应

目标：让可选 Sentinel Gateway 集成在真实触发 block 时返回稳定 JSON，而不是因 `Map.of(..., null)` 抛出运行时异常。

开发内容：

- 替换 `GatewaySentinelAutoConfiguration` 中不允许 null value 的 `Map.of` 构造。
- 增加测试直接执行 `GatewayCallbackManager` 中注册的 block handler，断言返回 429 和 JSON body。
- 保持未引入 Sentinel 或未启用 `maritime.gateway.sentinel.enabled=true` 时不激活。

验收标准：

- Sentinel block handler 返回 `{"code":429,"message":"FLOW_LIMITING","data":null}` 或等价稳定 JSON。
- handler 执行不抛 NPE。
- 可选依赖关闭路径不变。

验证命令：

- `mvn -pl platform-gateway-starter -Dtest=GatewaySentinelAutoConfigurationTest test`
- `mvn -pl platform-gateway-starter -am test`

## Final Verification

- `mvn -pl platform-gateway-starter -am test`
- 审阅 README 与设计稿是否仍存在 header、canonical string 或 auth-mode 语义矛盾。
- 确认新增测试至少覆盖：启动期 fail-closed、完整过滤链 header 生命周期、HMAC 无效签名不消费 nonce、TraceId 下游透传、Sentinel block handler 执行。
