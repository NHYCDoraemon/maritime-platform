---
task_id: MP-GWSTARTER-20260525A-T08
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 8
title: "[Gateway Starter] 实现 HMAC 系统请求认证链"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: high
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
  - L3
verification_commands:
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

实现系统对系统请求的 HMAC 认证链。

# Development Context

系统调用不依赖用户 JWT，应使用 appKey/appSecret 基于 canonical string 的 HMAC 签名，并结合 timestamp 和 nonce 防重放。

# Scope

- 新增 `HmacAuthenticationManager`。
- 新增 `HmacCanonicalRequestBuilder`。
- 读取 `X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。
- 校验 timestamp 窗口和 nonce 长度。
- Redis SETNX 防重放，key 为 `platform:gateway:hmac:nonce:{appKey}:{nonce}`。
- 按标准 canonical string 计算 HMAC-SHA256。
- 使用 constant-time compare 比对签名。
- 输出 `GatewayPrincipal.App`。

# Non-goals

- 不实现 app credential 存储管理。
- 不实现用户 JWT 与 HMAC 同时认证。
- 不接入业务权限接口。

# Implementation Detail

canonical string 必须稳定：appKey、method、rawPath、canonicalQuery、timestamp、nonce、bodyDigest。bodyDigest 应基于请求 body 的 SHA-256 hex。

# Acceptance

- 缺少 HMAC header 返回 `MISSING_HMAC_HEADERS`。
- timestamp 超窗返回 `TIMESTAMP_EXPIRED`。
- nonce 重复返回 `REPLAY_DETECTED`。
- bodyDigest 或 signature 不一致返回 `INVALID_SIGNATURE`。
- 成功后 exchange attribute 中存在应用 principal。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 canonical string、timestamp、nonce、bodyDigest、signature 成功和失败分支。

# Stop Conditions

- 如果请求 body 在 Gateway 中只能读取一次，停止并设计 body cache 方案，避免破坏下游转发。

# Executor Prompt Contract

实现标准 HMAC 链路，不兼容项目自定义签名格式。签名比较必须使用 constant-time compare。

