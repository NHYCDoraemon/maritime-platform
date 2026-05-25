---
task_id: MP-GWSTARTER-20260525A-T14
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 14
title: "[Gateway Starter] 补充单元测试和 Gateway 集成测试"
cycle: Gateway Starter MVP
module: verification
priority: P1
risk: high
depends_on:
  - MP-GWSTARTER-20260525A-T07
  - MP-GWSTARTER-20260525A-T09
  - MP-GWSTARTER-20260525A-T10
  - MP-GWSTARTER-20260525A-T11
  - MP-GWSTARTER-20260525A-T12
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

补齐 starter 单元测试和 Spring Cloud Gateway 集成测试。

# Development Context

gateway starter 属于平台基础安全能力，必须用测试锁定可复用行为，尤其是 nonce、防重放、可信 header 清理和上下文注入。

# Scope

- Route policy 匹配测试。
- JWT claims、state、nonce 测试。
- HMAC canonical string、timestamp、nonce、signature 测试。
- Trusted header strip 测试。
- Context header injection 测试。
- Spring Cloud Gateway 请求流测试。
- Testcontainers Redis 验证 nonce、session、blacklist、app credential。
- 验证 `none/jwt/hmac/jwt-or-hmac` 四种模式。
- 验证缺省配置可启动。

# Non-goals

- 不测试历史 gateway 迁移。
- 不测试业务权限。
- 不测试 UI 或管理后台。

# Implementation Detail

测试应优先覆盖安全失败分支和成功转发分支。集成测试使用最小测试 gateway route，不依赖真实 todo/iam/process 服务。

# Acceptance

- starter 单元测试和集成测试通过。
- 关键安全链路都有成功和失败分支覆盖。
- 新项目只引入 starter + 配置即可跑通测试 gateway。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 如使用 Testcontainers，确保本地 Docker 不可用时给出可诊断失败信息。

# Stop Conditions

- 如果测试依赖外部服务或真实 IAM 数据，停止并替换为本地 fake/mock/testcontainer。

# Executor Prompt Contract

补测试，不新增业务逻辑。测试必须证明 starter 是平台中立能力。

