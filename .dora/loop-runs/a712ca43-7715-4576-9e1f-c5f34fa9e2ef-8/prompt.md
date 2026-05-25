You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T08`
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
external_id: MP-GWSTARTER-20260525A-T08
external_source: dora-orchestrator
source_hash: 2595ccf02a99a744d5a537c8769a74c11ad917368e624f9fa602039872bafbd6
agent_hint: codex
risk: high
depends_on: 
  - MP-GWSTARTER-20260525A-T04
verification_level: 
  - L1
  - L2
  - L3
---

# Task Summary

实现系统对系统请求的 HMAC 认证链。

# Development Context

系统调用不依赖用户 JWT，应使用 appKey/appSecret 基于 canonical string 的 HMAC 签名，并结合 timestamp 和 nonce 防重放。

# Scope

- 新增 `HmacAuthenticationManager`。
- 新增 `HmacCanonicalRequestBuilder`。
- 读取 `X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。
- 校验 timestamp 窗口和 nonce 长度。
- Redis SETNX 防重放，key 为 `platform:gateway:hmac:nonce:{appKey}:{nonce}`。
- 按标准 canonical string 计算 HMAC-SHA256。
- 使用 constant-time compare 比对签名。
- 输出 `GatewayPrincipal.App`。

# Non-goals

- 不实现 app credential 存储管理。
- 不实现用户 JWT 与 HMAC 同时认证。
- 不接入业务权限接口。

# Implementation Detail

canonical string 必须稳定：appKey、method、rawPath、canonicalQuery、timestamp、nonce、bodyDigest。bodyDigest 应基于请求 body 的 SHA-256 hex。

# Acceptance

- 缺少 HMAC header 返回 `MISSING_HMAC_HEADERS`。
- timestamp 超窗返回 `TIMESTAMP_EXPIRED`。
- nonce 重复返回 `REPLAY_DETECTED`。
- bodyDigest 或 signature 不一致返回 `INVALID_SIGNATURE`。
- 成功后 exchange attribute 中存在应用 principal。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 canonical string、timestamp、nonce、bodyDigest、signature 成功和失败分支。

# Stop Conditions

- 如果请求 body 在 Gateway 中只能读取一次，停止并设计 body cache 方案，避免破坏下游转发。

# Executor Prompt Contract

实现标准 HMAC 链路，不兼容项目自定义签名格式。签名比较必须使用 constant-time compare。
