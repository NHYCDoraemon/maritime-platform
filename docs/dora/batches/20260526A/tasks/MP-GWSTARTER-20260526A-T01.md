---
task_id: MP-GWSTARTER-20260526A-T01
batch_id: 20260526A
program_prefix: GWSTARTER
sequence: 1
title: "[Gateway Starter Audit Fix] auth-mode 与启用开关 fail-closed"
cycle: Gateway Starter Audit Hardening
module: implementation
priority: P0
risk: high
depends_on: []
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md
source_summaries:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md
verification_level:
  - L1
  - L2
  - L3
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复认证模式需要 JWT/HMAC，但对应 `jwt.enabled` / `hmac.enabled` 未启用时过滤器不注册、请求静默放行的问题。

# Development Context

当前 `JwtAuthenticationGatewayFilter` 和 `HmacAuthenticationGatewayFilter` 都由 `@ConditionalOnProperty` 控制。若配置解析出 `AuthMode.JWT` 或 `AuthMode.HMAC`，但对应 enabled 为 false，`RouteSecurityPolicyFilter` 只写入 policy，不会阻止请求。

# Scope

- 启动期校验 default auth mode 与 route auth mode。
- `JWT` 要求 JWT enabled。
- `HMAC` 要求 HMAC enabled。
- `JWT_OR_HMAC` 要求 JWT 与 HMAC 都 enabled。
- `NONE` 保持可用。
- `JWT_AND_HMAC` 继续启动期拒绝。

# Non-goals

- 不实现 `JWT_AND_HMAC` 的双认证链路。
- 不改变 route path/method 匹配算法。
- 不新增业务权限、数据权限或历史 gateway 特判。

# Implementation Detail

在 `GatewaySecurityProperties.afterPropertiesSet()` 中集中检查所有会产生认证要求的配置项。默认认证模式和每个 route policy 都必须与对应 enabled 开关一致；错误信息要包含 default 或 route id，方便新项目启动期定位配置问题。同步补充自动装配测试，证明错误配置不会进入可转发状态。

# Acceptance

- 错误组合启动失败且错误信息说明具体 route/default auth mode。
- 显式 `default-auth-mode=none` 可启动。
- 自动装配测试证明不存在认证过滤器缺失却继续转发的配置。

# Verification

先运行聚焦测试，再运行完整 starter 测试。

# Stop Conditions

- 如果发现已有 README 推荐的最小配置会被新校验误伤，先更新文档和测试再继续。
- 如果某些历史消费者依赖 `default-auth-mode=jwt` 但未显式启用 JWT，停止并记录兼容迁移方案。

# Executor Prompt Contract

必须先补失败回归测试。不要通过默认注册空认证过滤器来“通过测试”；安全目标是错误配置 fail closed。
