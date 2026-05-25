---
task_id: MP-GWSTARTER-20260525A-T15
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 15
title: "[Gateway Starter] 可选集成 Sentinel Gateway 与 Knife4j Gateway 配置"
cycle: Gateway Starter MVP
module: implementation
priority: P3
risk: low
depends_on:
  - MP-GWSTARTER-20260525A-T01
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
verification_commands:
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

以可选配置方式评估并接入 Sentinel Gateway 与 Knife4j/OpenAPI 聚合辅助。

# Development Context

限流和接口文档聚合是 gateway 常见横向能力，但不能影响最小 starter 使用，也不能引入业务路由语义。

# Scope

- 评估并加入 Sentinel Gateway block handler 自动配置。
- 评估并加入 Knife4j/OpenAPI 聚合相关配置辅助。
- 配置默认关闭或条件启用。
- 未引入相关依赖时 starter 不报错。

# Non-goals

- 不配置业务路由。
- 不定义系统专属限流规则。
- 不强制所有项目引入 Sentinel 或 Knife4j。

# Implementation Detail

使用条件装配保护可选依赖。配置项应位于 `maritime.gateway.*` 下，默认不影响 JWT/HMAC 安全链。

# Acceptance

- 未引入 Sentinel/Knife4j 依赖时 starter 可正常启动。
- 引入依赖并开启配置后自动生效。
- 不引入业务路由语义。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖未引入依赖和启用配置两类场景。

# Stop Conditions

- 如果依赖版本会污染 BOM 或强制所有 gateway 引入额外栈，停止并拆成单独可选 starter。

# Executor Prompt Contract

这是可选增强任务。必须保持主 starter 最小使用路径不受影响。

