---
task_id: MP-GWSTARTER-20260525A-T03
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 3
title: "[Gateway Starter] 实现 route 级认证策略解析"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: medium
depends_on:
  - MP-GWSTARTER-20260525A-T02
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md
source_summaries:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md
verification_level:
  - L1
  - L2
verification_commands:
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

实现按路径和 method 解析认证模式的 `RouteSecurityPolicyResolver`。

# Development Context

新项目 gateway 不能再写自定义认证过滤器，必须通过配置表达哪些路径公开、哪些路径使用 JWT、HMAC 或混合模式。

# Scope

- 新增 `RouteSecurityPolicyResolver`。
- public paths 优先匹配为 `none`。
- route policy 支持 paths/methods 匹配。
- 未命中 route policy 时使用 `default-auth-mode`。
- 提供 `GatewaySecurityPolicyCustomizer` 扩展点。

# Non-goals

- 不解析业务权限。
- 不实现 JWT/HMAC 本身。
- 不维护动态路由表。

# Implementation Detail

优先使用 Spring Gateway/WebFlux 生态中稳定的 path matcher，避免手写字符串匹配。解析结果应是内部 policy 对象，供后续 filter 使用。

# Acceptance

- public path 一律解析为 `none`。
- route policy 能覆盖默认认证模式。
- 支持同一路径不同 method 配不同策略。
- 未命中 route policy 时回落默认模式。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 单元测试覆盖匹配优先级、method 过滤、默认回退和 customizer。

# Stop Conditions

- 如果 path matcher 与 Gateway 实际路由语义明显不一致，停止并改用 Gateway 原生匹配机制。

# Executor Prompt Contract

保证策略解析是纯平台能力，不引入项目业务路径。测试应明确保护 public path 优先级。

