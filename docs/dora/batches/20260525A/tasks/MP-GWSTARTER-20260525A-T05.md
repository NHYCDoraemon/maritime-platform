---
task_id: MP-GWSTARTER-20260525A-T05
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 5
title: "[Gateway Starter] 实现 JWT 用户认证链"
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

实现用户请求 JWT 提取、解密/验签、claims 映射和 principal 生成。

# Development Context

用户请求标准链是 `jwt + nonce/session/blacklist/user-enabled`。本任务只负责 JWT 基础认证，不负责 Redis 状态和 nonce。

# Scope

- 新增 `JwtAuthenticationManager`。
- 复用或适配平台现有 JWT/JWT 加解密能力。
- 支持 `Authorization: Bearer <token>`。
- 支持 encrypted JWT 配置。
- 校验签名、issuer、exp、clock skew。
- 新增 `JwtClaimsMapper` 扩展点。
- 输出 `GatewayPrincipal.User`。

# Non-goals

- 不做 session、blacklist、user-enabled。
- 不做业务权限查询。
- 不强制引入 `iam-sdk`。

# Implementation Detail

先检查平台已有 `JwtEncryptor` 或 token provider，优先复用现有实现。claims 字段名必须来自配置模型，不写死 IAM 内部 DTO。

# Acceptance

- 缺失 token 返回 `MISSING_TOKEN`。
- 非法 token 返回 `INVALID_TOKEN`。
- 过期 token 返回 `TOKEN_EXPIRED`。
- 缺少 userId/sessionId 时认证失败。
- 成功后 exchange attribute 中存在用户 principal。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 单元测试覆盖 token 缺失、非法、过期、claims 缺失和成功分支。

# Stop Conditions

- 如果现有 JWT 工具与 WebFlux/reactive 使用方式冲突，停止并记录适配方案，不要复制一套不兼容逻辑。

# Executor Prompt Contract

实现平台中立的 JWT 认证组件。不要把 todo、iam-center 或 process 的业务 claims 规则硬编码到 starter。

