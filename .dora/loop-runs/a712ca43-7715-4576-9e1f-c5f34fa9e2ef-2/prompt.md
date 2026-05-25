You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T02`
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
external_id: MP-GWSTARTER-20260525A-T02
external_source: dora-orchestrator
source_hash: fbb546fb87d0b9f0b83cfd9fc29d9029e9afb3099b439b9edf91c532144ffa28
agent_hint: codex
risk: medium
depends_on: 
  - MP-GWSTARTER-20260525A-T01
verification_level: 
  - L1
  - L2
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
