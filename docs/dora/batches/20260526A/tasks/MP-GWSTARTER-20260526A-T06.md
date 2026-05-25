---
task_id: MP-GWSTARTER-20260526A-T06
batch_id: 20260526A
program_prefix: GWSTARTER
sequence: 6
title: "[Gateway Starter Audit Fix] 修复 Sentinel block handler 运行时响应"
cycle: Gateway Starter Audit Hardening
module: implementation
priority: P1
risk: medium
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
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=GatewaySentinelAutoConfigurationTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 Sentinel Gateway block handler 在真实执行时因 `Map.of(..., null)` 抛出异常的问题。

# Development Context

`GatewaySentinelAutoConfiguration` 注册的 block handler 使用 `Map.of("code", 429, "message", "FLOW_LIMITING", "data", null)` 构造响应体。Java `Map.of` 不允许 null value，真实限流响应会抛出运行时异常。现有测试只验证 bean 激活，没有执行 handler。

# Scope

- 替换不允许 null 的 Map 构造方式。
- 增加 handler 执行测试，断言 429 和 JSON body。
- 保持 Sentinel 依赖缺失或开关关闭时不激活。

# Non-goals

- 不实现 Sentinel 规则管理 UI。
- 不改变未启用 Sentinel 时的 starter 自动装配行为。
- 不引入业务限流策略或业务路由语义。

# Implementation Detail

替换 `GatewaySentinelAutoConfiguration` 中的 `Map.of(..., null)`，使用允许 null 值的可序列化响应结构，或直接构造 JSON 响应。测试不仅要断言 bean 激活，还要调用 `GatewayCallbackManager` 注册的 block handler，读取响应状态和 body，确认真实限流路径不会抛出异常。

# Acceptance

- block handler 执行不抛 NPE。
- 响应体包含 `code=429`、`message=FLOW_LIMITING`、`data=null`。
- optional dependency 条件路径不变。

# Verification

先运行 Sentinel 聚焦测试，再运行完整 starter 测试。

# Stop Conditions

- 如果 handler 执行测试需要真实 Sentinel 网关上下文而当前测试环境无法构造，先记录最小可替代集成验证方案。
- 如果修复会激活未启用 Sentinel 的自动装配路径，停止并修正条件注解。

# Executor Prompt Contract

不要把 `data` 字段直接删掉绕过测试；错误响应契约需要保持与 gateway 默认错误响应一致。
