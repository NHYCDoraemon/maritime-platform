---
task_id: MP-GWSTARTER-20260525A-T10
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 10
title: "[Gateway Starter] 实现上下文 header 注入和扩展点"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: high
depends_on:
  - MP-GWSTARTER-20260525A-T05
  - MP-GWSTARTER-20260525A-T08
  - MP-GWSTARTER-20260525A-T09
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

实现认证后可信用户/应用上下文 header 注入和扩展点。

# Development Context

下游服务只能信任 gateway starter 验证后注入的 header。客户端传入的同名可信 header 必须先被清理。

# Scope

- 新增 `GatewayPrincipal` 模型。
- 新增 `TrustedHeaderWriter`。
- 新增 `GatewayPrincipalHeaderCustomizer`。
- JWT 成功后注入用户上下文 header。
- HMAC 成功后注入应用上下文 header。
- 确保注入发生在可信 header 清理之后。

# Non-goals

- 不调用 IAM 查询完整权限。
- 不注入业务特有 header。
- 不允许客户端原始可信 header 透传。

# Implementation Detail

JWT 注入 `X-User-Id`、`X-User-Name`、`X-Active-Org-Code`、`X-Active-Org-Name`、`X-Tenant-Id`、`X-Session-Id`、`X-System-Scope`、`X-User-Source`。HMAC 注入 `X-Verified-App-Code`、`X-App-Code`、`X-App-Id`、`X-Tenant-Code`、`X-Tenant-Id`、`X-App-Permissions`。

# Acceptance

- 客户端伪造的可信 header 被清理。
- 下游只收到 starter 认证后注入的 header。
- JWT 与 HMAC 注入字段互不污染。
- customizer 可追加项目字段。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 JWT/HMAC 注入、伪造 header 清理和 customizer。

# Stop Conditions

- 如果现有下游服务依赖其他历史 header，停止并记录兼容方案，不直接加入 starter 默认头集合。

# Executor Prompt Contract

保持 header 白名单和注入逻辑平台中立。不要在 starter 中注入权限查询结果，除非来源已经在 token claims 或 app credential 中。

