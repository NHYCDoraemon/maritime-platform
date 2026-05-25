---
task_id: MP-GWSTARTER-20260525A-T04
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 4
title: "[Gateway Starter] 实现基础 GlobalFilter 链和执行顺序"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: high
depends_on:
  - MP-GWSTARTER-20260525A-T03
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

实现 starter 的基础 GlobalFilter 链和顺序约束。

# Development Context

gateway starter 的核心是统一横向链路：TraceId、可信头清理、日志、认证策略、上下文注入。顺序错误会导致客户端伪造 header 或下游拿到错误上下文。

# Scope

- 新增 `TraceIdGatewayFilter`。
- 新增 `UntrustedHeaderStripFilter`。
- 新增 `RequestLogGatewayFilter`。
- 新增 `RouteSecurityPolicyFilter`。
- 新增 `ContextHeaderInjectionFilter`。
- 按设计顺序注册：0 TraceId、5 header strip、10 log、20 security、30 context injection。

# Non-goals

- 不在本任务实现 JWT/HMAC 完整认证。
- 不引入 Sentinel/Knife4j。
- 不记录敏感 token 或 signature。

# Implementation Detail

认证结果通过 exchange attribute 传递。`UntrustedHeaderStripFilter` 必须对所有路径执行，包括 public path。

# Acceptance

- 所有路径都会清理可信 header。
- public path 也会清理 `X-Internal-Call` 等可信头。
- filter order 有测试保护。
- 空认证实现下 gateway 可以正常转发 public path。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 测试可信头清理和 filter order。

# Stop Conditions

- 如果现有平台已经有相同 GlobalFilter order 常量，停止并复用或对齐已有常量。

# Executor Prompt Contract

优先保证链路骨架和安全顺序正确。不要把业务日志字段或项目特定 header 写进 starter。

