---
task_id: MP-GWSTARTER-20260525B-T03
batch_id: 20260525B
program_prefix: GWSTARTER
sequence: 3
title: "[Gateway Starter Fix] 修复 route auth-mode 缺失导致静默放行"
cycle: Gateway Starter Fixes
module: implementation
priority: P1
risk: high
depends_on: []
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-fix-batch.md
source_summaries:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-fix-batch.md
verification_level:
  - L1
  - L2
  - L3
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,RouteSecurityPolicyResolverTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 route policy 配置了 paths 但漏写 `auth-mode` 时静默放行的问题。

# Development Context

审计发现 `RoutePolicy.authMode` 可为 null。resolver 会把 null 策略放进 exchange，JWT/HMAC filter 发现不是自己的模式后直接放行。这是配置型安全绕过。

# Scope

- `RoutePolicy.authMode` 增加非空约束。
- `GatewaySecurityProperties.afterPropertiesSet()` 对 route auth-mode 做显式 fail-closed 校验。
- `RouteSecurityPolicyResolver.addRoutePolicy(..., null)` 显式拒绝。
- 补配置绑定和 resolver 回归测试。

# Non-goals

- 不新增业务权限策略。
- 不改变 `public-paths` 优先级。
- 不把漏配 route 自动回落 default auth mode。

# Implementation Detail

错误配置必须启动失败，而不是 fallback。启动失败信息需要包含 route id 和 `auth-mode`，方便新项目快速定位配置问题。

# Acceptance

- route 缺少 `auth-mode` 时启动期失败。
- programmatic route policy 传 null authMode 时抛 `IllegalArgumentException`。
- public paths 仍解析为 `NONE`。
- 未命中 route 仍使用 `default-auth-mode`。

# Verification

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,RouteSecurityPolicyResolverTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

# Stop Conditions

- 如果有既有配置依赖 route auth-mode 为空来表达默认认证，停止并记录迁移说明；不能保留静默放行。

# Executor Prompt Contract

这是 P1 fail-closed 修复。不要把 null auth-mode 解释成 `NONE`，也不要自动当作 default auth mode。

