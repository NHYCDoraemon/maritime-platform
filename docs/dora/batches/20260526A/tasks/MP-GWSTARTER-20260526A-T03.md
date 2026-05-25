---
task_id: MP-GWSTARTER-20260526A-T03
batch_id: 20260526A
program_prefix: GWSTARTER
sequence: 3
title: "[Gateway Starter Audit Fix] 补齐可信 header 清理并恢复 TraceId 下游透传"
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
  - mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,TrustedHeaderWriterTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

补齐设计稿中的可信 header 清理项，并确保 gateway 捕获的 traceId 继续透传给下游服务。

# Development Context

设计稿要求清理 `X-User-Permissions`、`X-Test-Channel`，当前实现未覆盖。当前 `TraceIdGatewayFilter` 捕获 `X-Trace-Id` 并写响应，但随后 `UntrustedHeaderStripFilter` 从请求中移除该 header，未再写回下游，导致下游 `platform-common-web` 不能复用 gateway traceId。

# Scope

- 清理清单补齐 `X-User-Permissions`、`X-Test-Channel`。
- 原始 `X-Trace-Id` 必须先被捕获、规范化，再由 gateway 写回下游请求。
- public path、JWT path、HMAC path 行为一致。
- 更新 README 清理清单和 TraceId 说明。

# Non-goals

- 不引入分布式追踪 SDK 或链路采样策略。
- 不修改下游 `platform-common-web` 的 servlet filter 行为。
- 不允许客户端原始 trace header 作为可信上下文直接绕过 gateway。

# Implementation Detail

在可信头清理列表补齐设计稿遗漏项，并在清理后由 gateway 重新写入 `TraceIdGatewayFilter.TRACE_ID_ATTR` 中的值。测试要覆盖客户端传入 traceId、客户端不传 traceId、public path、JWT path 和 HMAC path，断言下游请求和响应使用同一个 gateway 确认后的 traceId。

# Acceptance

- 伪造权限/测试通道 header 不会到达下游。
- 下游请求携带 gateway 确认后的 `X-Trace-Id`。
- 响应 `X-Trace-Id` 与下游请求一致。

# Verification

先运行过滤链聚焦测试，再运行完整 starter 测试。

# Stop Conditions

- 如果发现清理与重注入顺序会破坏现有 filter order，停止并先调整顺序测试。
- 如果下游 traceId 与响应 traceId 不一致，不能提交该任务。

# Executor Prompt Contract

不要为了透传 traceId 而信任客户端原始 header。必须先捕获/规范化，再由 gateway 写入。
