You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T01`
- **Batch**: `20260525A`
- **Branch / cwd**: `orchestrator/20260525A` (you are already inside the worktree at `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform`)

# Operating rules
1. **Decide and act.** The Issue Packet below is the contract. Make the most reasonable assumption it supports and proceed; do not ask clarifying questions -- there is no one to answer.
2. **Use tools to materialize work.** Edit / Write / Bash (and any agent skills you have) are how the run produces a diff. A text-only response leaves the worktree clean and the task is marked unverified.
3. **Stay in the worktree.** All edits go in `cwd`. Don't touch the parent repo checkout.
4. **Don't commit.** The orchestrator stages and commits after you exit. Just leave clean edits in the worktree.
5. **Stop conditions are real.** If the Issue Packet's Stop Conditions trigger, stop, document why in a TODO file in the worktree, and exit cleanly. Half-written placeholders beat zero output.
6. **Acceptance is the success bar.** When you believe each Acceptance bullet is satisfied, exit. The orchestrator will run any declared verification commands automatically.

# Skills you must NOT invoke

The business repo may ship a `dora-plane` skill that teaches agents to claim / heartbeat / release / comment via direct Plane API calls. **Do NOT invoke it on this run.** The orchestrator already owns those concerns:
- Claim was done by the orchestrator before you started.
- Release will be done by the orchestrator after you exit.
- Plane comments / labels / Pages are emitted by the orchestrator   with `dora-loop:*` markers -- adding more from inside the agent   produces duplicates and risks state-machine drift.

Just do the engineering work in the worktree and exit. Other skills (e.g. `superpowers:test-driven-development`, `superpowers:systematic-debugging`) are fine when contextually relevant.

---

# Issue Packet

---
external_id: MP-GWSTARTER-20260525A-T01
external_source: dora-orchestrator
source_hash: 5a4df820e480781e2d269db08bab9a5970f6042dab3706c0ef9609de8ed02f32
agent_hint: codex
risk: medium
depends_on: []
verification_level: 
  - L1
  - L2
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
