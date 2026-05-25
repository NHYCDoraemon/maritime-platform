---
task_id: MP-GWSTARTER-20260525A-T07
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 7
title: "[Gateway Starter] 实现 JWT nonce 防重放"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: high
depends_on:
  - MP-GWSTARTER-20260525A-T06
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md
source_summaries:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md
verification_level:
  - L1
  - L2
  - L3
verification_commands:
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

实现 JWT 用户请求的 nonce 防重放能力。

# Development Context

设计约定用户请求标准是 `jwt + nonce/session/blacklist`。nonce 不依赖 HMAC，写请求也应该具备基础防重放。

# Scope

- 新增 `JwtNonceValidator`。
- 默认实现 `simple-setnx` 模式。
- 默认对 `POST,PUT,PATCH,DELETE` 要求 `X-Nonce`。
- Redis key 使用 `platform:gateway:jwt:nonce:{sessionId}:{nonce}`。
- TTL 默认 5 分钟并支持配置。

# Non-goals

- 不实现 nonce pool。
- 不对 public path 强制 nonce。
- 不改变 JWT session 校验逻辑。

# Implementation Detail

使用 Redis SETNX 语义，一次性写入并带 TTL。nonce 长度和 header 名后续可从配置扩展，但第一版先保持设计约定。

# Acceptance

- 写请求缺少 nonce 返回 `NONCE_REQUIRED`。
- 重复 nonce 返回 `REPLAY_DETECTED`。
- TTL 过期后 nonce 可重新使用。
- GET 等未配置 method 默认不要求 nonce。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖成功、重复、缺失和 method 不要求四类测试。

# Stop Conditions

- 如果 Reactive Redis SETNX with TTL 无法一次性保证原子语义，停止并改用 Lua 或可靠原子 API。

# Executor Prompt Contract

实现 simple-setnx，不提前扩展复杂 nonce pool。失败必须返回统一错误码。

