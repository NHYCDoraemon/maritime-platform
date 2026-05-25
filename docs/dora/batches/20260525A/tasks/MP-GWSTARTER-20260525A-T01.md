---
task_id: MP-GWSTARTER-20260525A-T01
batch_id: 20260525A
program_prefix: GWSTARTER
sequence: 1
title: "[Gateway Starter] 新增 platform-gateway-starter 模块骨架"
cycle: Gateway Starter MVP
module: implementation
priority: P1
risk: medium
depends_on: []
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

建立 `platform-gateway-starter` 基础模块、依赖管理和自动装配入口。

# Development Context

当前平台需要把 todo/iam/process 未来共用的 gateway 横向能力抽象成独立 starter。第一步是让模块进入根工程和 BOM，并具备 Spring Boot 自动装配入口。

# Scope

- 在根 `pom.xml` 增加 `platform-gateway-starter` module。
- 在 `platform-bom/pom.xml` 管理 starter 版本。
- 新建 `platform-gateway-starter/pom.xml`。
- 引入 Spring Cloud Gateway、Spring Boot autoconfigure、Reactive Redis、Actuator 相关依赖。
- 增加 `GatewayAutoConfiguration` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 建立设计文档中的包结构。

# Non-goals

- 不实现任何认证逻辑。
- 不引入 todo、IAM 或 process 业务规则。
- 不创建项目 gateway 示例模块。

# Implementation Detail

按平台现有 Maven module 和 starter 命名方式落地。自动配置类只做空骨架和条件装配准备，避免在本任务中引入半成品过滤器。

# Acceptance

- 根工程能识别 `platform-gateway-starter` 模块。
- BOM 能管理 starter artifact。
- `GatewayAutoConfiguration` 可被 Spring Boot 自动装配机制发现。
- 包结构与设计文档一致。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 检查根 `pom.xml` 和 `platform-bom/pom.xml` 是否包含 starter。

# Stop Conditions

- 如果平台当前 Spring Boot 或 Spring Cloud Gateway 版本无法确定，停止并先确认版本管理策略。
- 如果根工程 Maven 结构与预期不一致，停止并记录需要调整的父子模块关系。

# Executor Prompt Contract

只提交模块骨架和自动装配入口，不实现认证过滤器。保持变更可编译，并遵循仓库现有 Maven 和包命名风格。

