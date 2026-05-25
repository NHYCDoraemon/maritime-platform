You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T12`
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
external_id: MP-GWSTARTER-20260525A-T12
external_source: dora-orchestrator
source_hash: 462ccc50b1258014f859618e776a04b80fc271e7a681ac1fa064ebb6f535967f
agent_hint: codex
risk: medium
depends_on: 
  - MP-GWSTARTER-20260525A-T05
  - MP-GWSTARTER-20260525A-T08
  - MP-GWSTARTER-20260525A-T11
verification_level: 
  - L1
  - L2
---

# Task Summary

实现 `jwt-or-hmac` 混合模式，并对 `jwt-and-hmac` 做明确预留。

# Development Context

部分路径可能同时支持用户请求和系统请求。高安全代理/代办请求未来可能需要同时验证 JWT 与 HMAC，但第一版不能误开放未完成能力。

# Scope

- 在 `RouteSecurityPolicyFilter` 中实现 `jwt-or-hmac`。
- 有 Bearer token 时走 JWT。
- 无 Bearer token 时走 HMAC。
- `jwt-and-hmac` 可被配置模型识别。
- 第一版对 `jwt-and-hmac` 返回明确错误或启动期拒绝业务使用。

# Non-goals

- 不实现真正的 JWT + HMAC 双认证。
- 不实现代理/代办业务语义。
- 不做权限提升逻辑。

# Implementation Detail

`jwt-or-hmac` 的选择逻辑只能基于是否存在 Bearer token，不做猜测。`jwt-and-hmac` 必须是显式保留行为，避免被误当作 none 或 jwt-or-hmac。

# Acceptance

- `jwt-or-hmac` 下 Bearer token 成功时注入用户上下文。
- `jwt-or-hmac` 下无 Bearer token 时可通过 HMAC。
- 两种路径失败时返回对应 JWT/HMAC 错误。
- `jwt-and-hmac` 不会被误开放。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 JWT 优先、HMAC fallback、双失败、保留模式。

# Stop Conditions

- 如果产品需要第一版支持真实代理/代办双认证，停止并重新拆分安全设计，不在本任务临时拼接。

# Executor Prompt Contract

实现混合模式和保留保护。不要把 `jwt-and-hmac` 做成半成品可用能力。
