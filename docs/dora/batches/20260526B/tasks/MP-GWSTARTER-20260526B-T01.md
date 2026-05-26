---
task_id: MP-GWSTARTER-20260526B-T01
batch_id: 20260526B
program_prefix: GWSTARTER
sequence: 1
title: "[Gateway Starter Post-Audit] 补齐 programmatic route policy fail-closed"
cycle: Gateway Starter Post-Audit Hardening
module: implementation
priority: P0
risk: high
depends_on: []
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
  - platform-gateway-starter/README.md
source_docs:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md
source_summaries:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md
required_skills:
  - maritime-java-backend-development
  - maritime-platform-governance
  - superpowers:test-driven-development
verification_level:
  - L1
  - L2
  - L3
verification_commands:
  - java -version
  - mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 `GatewaySecurityPolicyCustomizer` / `RouteSecurityPolicyResolver.addRoutePolicy()` 添加认证路由时绕过 fail-closed 校验的问题。

# Development Context

`GatewaySecurityProperties.afterPropertiesSet()` 已经校验 default auth mode 和 properties routes，但 programmatic routes 在 `RouteSecurityPolicyResolver` 中编译，当前只拒绝 `JWT_AND_HMAC`，没有校验 `jwt.enabled`、`hmac.enabled`。如果 programmatic route 要求 `JWT` 或 `HMAC`，但对应过滤器未注册，请求可能带着认证 policy 继续转发。

# Scope

- 抽出或复用统一的 auth-mode 与 enabled 开关一致性校验逻辑。
- 覆盖 `default-auth-mode`、properties routes、programmatic routes。
- `JWT` 要求 `jwt.enabled=true`。
- `HMAC` 要求 `hmac.enabled=true`。
- `JWT_OR_HMAC` 要求 JWT 与 HMAC 都启用。
- `NONE` 保持可用。
- `JWT_AND_HMAC` 继续启动期拒绝。

# Non-goals

- 不实现 `JWT_AND_HMAC` 双认证。
- 不改变 route path/method 匹配优先级。
- 不为禁用的认证模式注册空过滤器。

# Implementation Detail

先补失败回归测试：用 `ApplicationContextRunner` 注册 `GatewaySecurityPolicyCustomizer`，分别添加 `JWT`、`HMAC`、`JWT_OR_HMAC` programmatic route，同时保持对应 enabled 开关为 false，断言 Spring context 启动失败且错误信息包含 route id 和 auth mode。

实现时建议把当前 `GatewaySecurityProperties` 内部的模式一致性校验提取成包内可复用组件，或由 `RouteSecurityPolicyResolver` 在 customizers 执行后统一校验 `programmaticRoutes`。错误信息必须能定位来源，至少包含 `programmatic route`、route id、auth mode 和缺失的 enabled 开关。

# Acceptance

- programmatic `JWT` route 且 `jwt.enabled=false` 时启动失败。
- programmatic `HMAC` route 且 `hmac.enabled=false` 时启动失败。
- programmatic `JWT_OR_HMAC` route 只启用一个认证组件时启动失败。
- programmatic `NONE` route 仍可启动。
- properties route 和 default auth mode 的既有 fail-closed 测试继续通过。

# Verification

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test`
- `mvn -pl platform-gateway-starter -am test`

# Stop Conditions

- 如果发现 programmatic route 设计上允许在运行期动态切换 enabled 开关，停止并记录兼容方案；不要把认证缺失路径放行。
- 如果现有测试依赖未启用认证组件的 programmatic `JWT` 或 `HMAC` route，需要先修正测试语义，再改实现。

# Executor Prompt Contract

这是 P0 安全修复。必须先写失败测试，再改实现。不要用空认证过滤器、默认放行或降低 route policy 的方式掩盖问题。
