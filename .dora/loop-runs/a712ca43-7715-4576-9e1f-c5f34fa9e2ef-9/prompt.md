You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T09`
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
external_id: MP-GWSTARTER-20260525A-T09
external_source: dora-orchestrator
source_hash: c736a07ad0910fecf901db759098c53ed1e5a6526518936208969e09840fc588
agent_hint: codex
risk: medium
depends_on: 
  - MP-GWSTARTER-20260525A-T08
verification_level: 
  - L1
  - L2
---

# Task Summary

实现 HMAC appKey 对应 appSecret 和应用上下文的解析能力。

# Development Context

HMAC 需要通过 appKey 找到 appSecret，并把 appCode、tenant、permissions 等应用上下文传递给下游。第一版默认 Redis + 配置 fallback，并允许自定义 resolver。

# Scope

- 新增 `AppCredentialResolver` 接口。
- 实现默认 resolver。
- 默认读取 Redis hash：`iam:app:auth:{appKey}`。
- 支持字段映射：`appSecret`、`appCode`、`appId`、`tenantId`、`tenantCode`、`permissions`、`isEnabled`。
- 支持配置 fallback apps。
- 支持自定义 bean 覆盖默认 resolver。

# Non-goals

- 不实现 app 管理后台。
- 不调用 IAM SDK 查询权限。
- 不加业务缓存失效协议。

# Implementation Detail

resolver 返回平台中立 credential 对象，供 HMAC manager 进行签名校验和 principal 构建。Redis 字段名全部来自配置。

# Acceptance

- Redis 中不存在 appKey 返回 `UNKNOWN_APP`。
- disabled app 返回 `APP_DISABLED`。
- Redis 不存在时可使用配置 fallback。
- 自定义 `AppCredentialResolver` bean 可覆盖默认实现。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 覆盖 Redis 命中、Redis 未命中、fallback、disabled、自定义 bean。

# Stop Conditions

- 如果 IAM 现有 app auth key 结构与设计字段差异较大，停止并补充字段映射，不要写死当前字段。

# Executor Prompt Contract

只实现凭证解析抽象和默认实现。保持字段映射配置化，避免 starter 依赖 IAM 业务模型。
