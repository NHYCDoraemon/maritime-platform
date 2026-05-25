You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T15`
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
external_id: MP-GWSTARTER-20260525A-T15
external_source: dora-orchestrator
source_hash: ea6ae6beeb706a2174e8adb080c57eb0d46155977ce38d704827d28e26fd97d9
agent_hint: codex
risk: low
depends_on: 
  - MP-GWSTARTER-20260525A-T01
  - MP-GWSTARTER-20260525A-T04
verification_level: 
  - L1
  - L2
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
