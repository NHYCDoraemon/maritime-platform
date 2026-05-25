You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T13`
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
external_id: MP-GWSTARTER-20260525A-T13
external_source: dora-orchestrator
source_hash: dfef0aed09eb0f1561629b5de06e58a48c495e9df60d5b1b35dc15bc04e69296
agent_hint: codex
risk: low
depends_on: 
  - MP-GWSTARTER-20260525A-T02
  - MP-GWSTARTER-20260525A-T10
  - MP-GWSTARTER-20260525A-T11
verification_level: 
  - L1
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
