You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T14`
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
external_id: MP-GWSTARTER-20260525A-T14
external_source: dora-orchestrator
source_hash: c6f0f14c5e02ddd6d66fe0be20d9811f480358d8e7b537f7340b353548577ef9
agent_hint: codex
risk: high
depends_on: 
  - MP-GWSTARTER-20260525A-T07
  - MP-GWSTARTER-20260525A-T09
  - MP-GWSTARTER-20260525A-T10
  - MP-GWSTARTER-20260525A-T11
  - MP-GWSTARTER-20260525A-T12
verification_level: 
  - L1
  - L2
  - L3
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
