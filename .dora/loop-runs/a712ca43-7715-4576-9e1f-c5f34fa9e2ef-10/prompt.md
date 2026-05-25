You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T10`
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
external_id: MP-GWSTARTER-20260525A-T10
external_source: dora-orchestrator
source_hash: 1e769ec406ac22b62ce53e685b504818d31f8778b1c877d72da1afde247b8ba4
agent_hint: codex
risk: high
depends_on: 
  - MP-GWSTARTER-20260525A-T05
  - MP-GWSTARTER-20260525A-T08
  - MP-GWSTARTER-20260525A-T09
verification_level: 
  - L1
  - L2
  - L3
---

# Task Summary

实现认证后可信用户/应用上下文 header 注入和扩展点。

# Development Context

下游服务只能信任 gateway starter 验证后注入的 header。客户端传入的同名可信 header 必须先被清理。

# Scope

- 新增 `GatewayPrincipal` 模型。
- 新增 `TrustedHeaderWriter`。
- 新增 `GatewayPrincipalHeaderCustomizer`。
- JWT 成功后注入用户上下文 header。
- HMAC 成功后注入应用上下文 header。
- 确保注入发生在可信 header 清理之后。

# Non-goals

- 不调用 IAM 查询完整权限。
- 不注入业务特有 header。
- 不允许客户端原始可信 header 透传。

# Implementation Detail

JWT 注入 `X-User-Id`、`X-User-Name`、`X-Active-Org-Code`、`X-Active-Org-Name`、`X-Tenant-Id`、`X-Session-Id`、`X-System-Scope`、`X-User-Source`。HMAC 注入 `X-Verified-App-Code`、`X-App-Code`、`X-App-Id`、`X-Tenant-Code`、`X-Tenant-Id`、`X-App-Permissions`。

# Acceptance

- 客户端伪造的可信 header 被清理。
- 下游只收到 starter 认证后注入的 header。
- JWT 与 HMAC 注入字段互不污染。
- customizer 可追加项目字段。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 JWT/HMAC 注入、伪造 header 清理和 customizer。

# Stop Conditions

- 如果现有下游服务依赖其他历史 header，停止并记录兼容方案，不直接加入 starter 默认头集合。

# Executor Prompt Contract

保持 header 白名单和注入逻辑平台中立。不要在 starter 中注入权限查询结果，除非来源已经在 token claims 或 app credential 中。
