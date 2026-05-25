You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T06`
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
external_id: MP-GWSTARTER-20260525A-T06
external_source: dora-orchestrator
source_hash: a1328af2c26d9db328de79f8fd6bee01045d3d75c52bd11f172d0ad0bc9513ab
agent_hint: codex
risk: high
depends_on: 
  - MP-GWSTARTER-20260525A-T05
verification_level: 
  - L1
  - L2
  - L3
---

# Task Summary

为 JWT 用户 principal 增加 session、blacklist、user-enabled Redis 状态校验。

# Development Context

JWT 本身只证明 token 有效，不代表 session 仍有效、token 未被拉黑、用户仍启用。平台 starter 需要统一这部分安全状态校验。

# Scope

- 新增 `JwtStateValidator`。
- 使用 Reactive Redis 校验 `iam:session:{sessionId}`。
- 使用 Reactive Redis 校验 `iam:token:blacklist:{jti}`。
- 使用 Reactive Redis 校验 `iam:user:enabled:{userId}`。
- 支持 `require-session`、`check-blacklist`、`check-user-enabled` 开关。
- 支持 `user-enabled-disabled-value`。

# Non-goals

- 不实现 session 创建或注销接口。
- 不调用 IAM query-service。
- 不处理业务权限。

# Implementation Detail

Redis key 前缀来自配置。校验组件应返回明确错误码，供统一错误 writer 输出。

# Acceptance

- session 不存在返回 `SESSION_EXPIRED`。
- blacklist 命中返回 `TOKEN_BLACKLISTED`。
- user disabled 返回 `USER_DISABLED`。
- 三类校验开关可独立关闭。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 使用 mock 或 embedded/test Redis 覆盖每个分支。

# Stop Conditions

- 如果现有 Redis 序列化策略影响读取简单字符串值，停止并对齐平台 Redis template 用法。

# Executor Prompt Contract

只做状态校验，不做状态写入。保持 Redis key 和字段完全配置化。
