---
task_id: MP-GWSTARTER-20260525A-T09
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 9
title: "[Gateway Starter] 实现 AppCredentialResolver"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: medium
depends_on:
  - MP-GWSTARTER-20260525A-T08
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

实现 HMAC appKey 对应 appSecret 和应用上下文的解析能力。

# Development Context

HMAC 需要通过 appKey 找到 appSecret，并把 appCode、tenant、permissions 等应用上下文传递给下游。第一版默认 Redis + 配置 fallback，并允许自定义 resolver。

# Scope

- 新增 `AppCredentialResolver` 接口。
- 实现默认 resolver。
- 默认读取 Redis hash：`iam:app:auth:{appKey}`。
- 支持字段映射：`appSecret`、`appCode`、`appId`、`tenantId`、`tenantCode`、`permissions`、`isEnabled`。
- 支持配置 fallback apps。
- 支持自定义 bean 覆盖默认 resolver。

# Non-goals

- 不实现 app 管理后台。
- 不调用 IAM SDK 查询权限。
- 不加业务缓存失效协议。

# Implementation Detail

resolver 返回平台中立 credential 对象，供 HMAC manager 进行签名校验和 principal 构建。Redis 字段名全部来自配置。

# Acceptance

- Redis 中不存在 appKey 返回 `UNKNOWN_APP`。
- disabled app 返回 `APP_DISABLED`。
- Redis 不存在时可使用配置 fallback。
- 自定义 `AppCredentialResolver` bean 可覆盖默认实现。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 Redis 命中、Redis 未命中、fallback、disabled、自定义 bean。

# Stop Conditions

- 如果 IAM 现有 app auth key 结构与设计字段差异较大，停止并补充字段映射，不要写死当前字段。

# Executor Prompt Contract

只实现凭证解析抽象和默认实现。保持字段映射配置化，避免 starter 依赖 IAM 业务模型。

