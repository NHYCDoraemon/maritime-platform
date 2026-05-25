---
task_id: MP-GWSTARTER-20260525A-T06
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 6
title: "[Gateway Starter] 实现 JWT session/blacklist/user-enabled 状态校验"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: high
depends_on:
  - MP-GWSTARTER-20260525A-T05
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

为 JWT 用户 principal 增加 session、blacklist、user-enabled Redis 状态校验。

# Development Context

JWT 本身只证明 token 有效，不代表 session 仍有效、token 未被拉黑、用户仍启用。平台 starter 需要统一这部分安全状态校验。

# Scope

- 新增 `JwtStateValidator`。
- 使用 Reactive Redis 校验 `iam:session:{sessionId}`。
- 使用 Reactive Redis 校验 `iam:token:blacklist:{jti}`。
- 使用 Reactive Redis 校验 `iam:user:enabled:{userId}`。
- 支持 `require-session`、`check-blacklist`、`check-user-enabled` 开关。
- 支持 `user-enabled-disabled-value`。

# Non-goals

- 不实现 session 创建或注销接口。
- 不调用 IAM query-service。
- 不处理业务权限。

# Implementation Detail

Redis key 前缀来自配置。校验组件应返回明确错误码，供统一错误 writer 输出。

# Acceptance

- session 不存在返回 `SESSION_EXPIRED`。
- blacklist 命中返回 `TOKEN_BLACKLISTED`。
- user disabled 返回 `USER_DISABLED`。
- 三类校验开关可独立关闭。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 使用 mock 或 embedded/test Redis 覆盖每个分支。

# Stop Conditions

- 如果现有 Redis 序列化策略影响读取简单字符串值，停止并对齐平台 Redis template 用法。

# Executor Prompt Contract

只做状态校验，不做状态写入。保持 Redis key 和字段完全配置化。

