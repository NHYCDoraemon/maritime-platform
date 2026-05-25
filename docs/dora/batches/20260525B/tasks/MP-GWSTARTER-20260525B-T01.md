---
task_id: MP-GWSTARTER-20260525B-T01
batch_id: 20260525B
program_prefix: GWSTARTER
sequence: 1
title: "[Gateway Starter Fix] 修复 HMAC 签名 header 被预认证清理破坏"
cycle: Gateway Starter Fixes
module: implementation
priority: P0
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
  - mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 `UntrustedHeaderStripFilter` 在 HMAC 认证前移除 `X-App-Key`，导致系统请求无法认证的问题。

# Development Context

审计发现过滤器顺序是 header strip 先于 HMAC auth。当前清理清单包含 `X-App-Key`，而 HMAC auth 需要从 `X-App-Key` 读取 appKey，因此真实 HMAC 请求会被错误判定为 `MISSING_HMAC_HEADERS`。

# Scope

- 区分“入站 HMAC 签名 header”和“下游可信上下文 header”。
- 预认证清理阶段保留 HMAC 签名所需 header。
- HMAC 认证成功后，转发下游前移除原始签名 header。
- 补完整过滤链回归测试。

# Non-goals

- 不改变 HMAC canonical string。
- 不新增业务 app 权限逻辑。
- 不改变 JWT 路径的 nonce header 处理。

# Implementation Detail

修改 `UntrustedHeaderStripFilter`，不要在预认证阶段移除配置默认 HMAC 签名 header：`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。保留对下游上下文 header 的清理：`X-App-Code`、`X-App-Id`、`X-Verified-App-Code`、`X-App-Permissions`、tenant/user/internal headers。

修改 `HmacAuthenticationGatewayFilter`，认证成功后基于配置移除原始 HMAC 签名 header，再让 `ContextHeaderInjectionFilter` 注入可信应用上下文。

# Acceptance

- 完整过滤链下 HMAC 请求不会因为 `X-App-Key` 被清理而失败。
- HMAC 认证成功后，下游不会收到原始签名 header。
- 下游仍能收到 `X-Verified-App-Code`、`X-App-Code`、`X-App-Id`、`X-Tenant-Code`、`X-Tenant-Id`、`X-App-Permissions`。

# Verification

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

# Stop Conditions

- 如果发现下游已有服务依赖原始 `X-App-Key`，停止并记录兼容方案；不要直接把原始签名 header 作为可信上下文继续透传。

# Executor Prompt Contract

这是 P0 安全链路修复。必须先补失败回归测试，再改代码。不要把 HMAC 入站签名 header 加入 trusted context。

