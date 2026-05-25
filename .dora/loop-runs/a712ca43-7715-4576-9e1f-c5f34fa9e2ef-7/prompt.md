You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T07`
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
external_id: MP-GWSTARTER-20260525A-T07
external_source: dora-orchestrator
source_hash: 1aefb1e769ea9c718b7c34e3bb8b63bac8bf8e55733b05998fb022bfaf8d38e9
agent_hint: codex
risk: high
depends_on: 
  - MP-GWSTARTER-20260525A-T06
verification_level: 
  - L1
  - L2
  - L3
---

# Task Summary

实现 JWT 用户请求的 nonce 防重放能力。

# Development Context

设计约定用户请求标准是 `jwt + nonce/session/blacklist`。nonce 不依赖 HMAC，写请求也应该具备基础防重放。

# Scope

- 新增 `JwtNonceValidator`。
- 默认实现 `simple-setnx` 模式。
- 默认对 `POST,PUT,PATCH,DELETE` 要求 `X-Nonce`。
- Redis key 使用 `platform:gateway:jwt:nonce:{sessionId}:{nonce}`。
- TTL 默认 5 分钟并支持配置。

# Non-goals

- 不实现 nonce pool。
- 不对 public path 强制 nonce。
- 不改变 JWT session 校验逻辑。

# Implementation Detail

使用 Redis SETNX 语义，一次性写入并带 TTL。nonce 长度和 header 名后续可从配置扩展，但第一版先保持设计约定。

# Acceptance

- 写请求缺少 nonce 返回 `NONCE_REQUIRED`。
- 重复 nonce 返回 `REPLAY_DETECTED`。
- TTL 过期后 nonce 可重新使用。
- GET 等未配置 method 默认不要求 nonce。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖成功、重复、缺失和 method 不要求四类测试。

# Stop Conditions

- 如果 Reactive Redis SETNX with TTL 无法一次性保证原子语义，停止并改用 Lua 或可靠原子 API。

# Executor Prompt Contract

实现 simple-setnx，不提前扩展复杂 nonce pool。失败必须返回统一错误码。
