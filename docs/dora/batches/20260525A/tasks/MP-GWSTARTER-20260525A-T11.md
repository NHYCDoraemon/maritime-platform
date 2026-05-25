---
task_id: MP-GWSTARTER-20260525A-T11
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 11
title: "[Gateway Starter] 实现统一错误响应"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: medium
depends_on:
  - MP-GWSTARTER-20260525A-T04
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

实现 gateway 认证失败时的一致 JSON 错误响应。

# Development Context

starter 中 JWT、HMAC、nonce、session 等失败分支都需要统一响应格式，避免各项目自行拼接错误体。

# Scope

- 新增 `GatewayErrorWriter` 默认实现。
- 输出 `{"code":401,"message":"MISSING_TOKEN","data":null}` 形态。
- 响应头包含 `X-Trace-Id`。
- 覆盖设计文档列出的认证错误码。
- 支持自定义 `GatewayErrorWriter` bean 覆盖默认实现。

# Non-goals

- 不实现业务异常处理。
- 不改变下游服务错误响应。
- 不记录敏感认证材料。

# Implementation Detail

认证组件应抛出或返回统一网关安全错误对象，由 writer 负责状态码和 JSON 输出。401/403 语义必须稳定。

# Acceptance

- 所有认证失败路径都可通过统一 writer 输出。
- 401/403 状态码与错误码语义一致。
- 响应包含 `X-Trace-Id`。
- 自定义 writer 可覆盖默认实现。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖典型 JWT、HMAC、nonce、forbidden 错误响应。

# Stop Conditions

- 如果平台已有统一错误响应模型，停止并对齐现有模型，避免 gateway starter 定义第二套响应协议。

# Executor Prompt Contract

只处理 gateway 安全失败。不要吞掉下游服务响应，也不要扩展业务错误码。

