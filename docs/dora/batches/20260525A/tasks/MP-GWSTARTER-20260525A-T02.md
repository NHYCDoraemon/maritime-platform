---
task_id: MP-GWSTARTER-20260525A-T02
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 2
title: "[Gateway Starter] 实现网关安全配置模型"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: medium
depends_on:
  - MP-GWSTARTER-20260525A-T01
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

实现 `maritime.gateway.security.*` 的统一配置模型。

# Development Context

starter 需要通过配置表达 public paths、route 策略、JWT、HMAC、nonce、Redis key、header 和 credential fallback。配置模型是后续所有过滤器和认证组件的输入契约。

# Scope

- 新增 `GatewaySecurityProperties`。
- 实现 `none`、`jwt`、`hmac`、`jwt-or-hmac`，预留 `jwt-and-hmac`。
- 实现 public paths、default auth mode、route policies 配置。
- 实现 JWT 配置：encrypted、secret、issuer、clock skew、claims mapping、Redis keys、validation、nonce。
- 实现 HMAC 配置：timestamp tolerance、nonce、headers、credential source、Redis fields、fallback apps。
- 增加配置校验。

# Non-goals

- 不实现认证执行逻辑。
- 不读取 IAM 远程接口。
- 不提供历史项目兼容 preset。

# Implementation Detail

使用 `@ConfigurationProperties(prefix = "maritime.gateway.security")`。配置字段应采用平台常用的 kebab-case 绑定方式，枚举值兼容小写配置。

# Acceptance

- 缺省配置可以启动。
- 启用 JWT/HMAC 但缺少关键参数时启动期报清晰错误。
- `jwt-and-hmac` 可绑定但不会被标记为已完成能力。
- 配置类有绑定测试覆盖。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖正常配置、缺省配置、错误配置三类测试。

# Stop Conditions

- 如果发现平台已有同名配置前缀冲突，停止并提出兼容命名调整。

# Executor Prompt Contract

只建立配置契约和校验，不实现认证业务。字段命名必须与设计文档保持一致，避免后续任务反复改配置。

