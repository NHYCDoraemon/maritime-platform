---
task_id: MP-GWSTARTER-20260525A-T13
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 13
title: "[Gateway Starter] 补充 starter 文档和新项目接入样例"
cycle: Gateway Starter MVP
module: planning
priority: P2
risk: low
depends_on:
  - MP-GWSTARTER-20260525A-T02
  - MP-GWSTARTER-20260525A-T10
  - MP-GWSTARTER-20260525A-T11
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md
source_summaries:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md
verification_level:
  - L1
verification_commands:
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

补充 starter README 和新项目最小接入样例。

# Development Context

平台 starter 的价值在于新项目少开发。文档必须明确“引入 starter + 配置即可”，并说明与 `iam-sdk` 的关系。

# Scope

- 新增 starter README。
- 提供最小 gateway `application.yml` 示例。
- 说明 JWT + nonce/session/blacklist/user-enabled 用户请求标准。
- 说明 HMAC + timestamp/nonce/bodyDigest 系统请求标准。
- 说明下游需要租户、用户、应用权限时可手动引入 `iam-sdk` 或业务 SDK。
- 说明 starter 不包含 todo/IAM/process 业务特殊规则。

# Non-goals

- 不写迁移手册。
- 不写历史 gateway 对照改造步骤。
- 不承诺 starter 自动引入 `iam-sdk`。

# Implementation Detail

README 面向新项目开发者，给出最小依赖、启动类、配置样例、header 清理/注入清单和常见错误码。

# Acceptance

- README 包含最小可运行配置。
- README 明确 starter 与 `iam-sdk` 的关系。
- README 明确哪些 header 会被清理和注入。
- README 明确 `jwt-and-hmac` 是预留能力。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test` 确认文档变更不破坏工程。
- 人工检查 README 是否覆盖接入者需要的最小信息。

# Stop Conditions

- 如果实际配置字段和 Task 2 产生差异，停止并先同步配置字段。

# Executor Prompt Contract

写接入文档，不写迁移文档。文档要服务新项目复用，避免讲历史项目细节。

