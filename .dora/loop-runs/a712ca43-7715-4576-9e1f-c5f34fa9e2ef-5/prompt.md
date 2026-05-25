You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T05`
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
external_id: MP-GWSTARTER-20260525A-T05
external_source: dora-orchestrator
source_hash: c9b3e5f5e6e8afa6651b054832c73c7d7ab287ee28a24294be8f1f87944f439a
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

实现用户请求 JWT 提取、解密/验签、claims 映射和 principal 生成。

# Development Context

用户请求标准链是 `jwt + nonce/session/blacklist/user-enabled`。本任务只负责 JWT 基础认证，不负责 Redis 状态和 nonce。

# Scope

- 新增 `JwtAuthenticationManager`。
- 复用或适配平台现有 JWT/JWT 加解密能力。
- 支持 `Authorization: Bearer <token>`。
- 支持 encrypted JWT 配置。
- 校验签名、issuer、exp、clock skew。
- 新增 `JwtClaimsMapper` 扩展点。
- 输出 `GatewayPrincipal.User`。

# Non-goals

- 不做 session、blacklist、user-enabled。
- 不做业务权限查询。
- 不强制引入 `iam-sdk`。

# Implementation Detail

先检查平台已有 `JwtEncryptor` 或 token provider，优先复用现有实现。claims 字段名必须来自配置模型，不写死 IAM 内部 DTO。

# Acceptance

- 缺失 token 返回 `MISSING_TOKEN`。
- 非法 token 返回 `INVALID_TOKEN`。
- 过期 token 返回 `TOKEN_EXPIRED`。
- 缺少 userId/sessionId 时认证失败。
- 成功后 exchange attribute 中存在用户 principal。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 单元测试覆盖 token 缺失、非法、过期、claims 缺失和成功分支。

# Stop Conditions

- 如果现有 JWT 工具与 WebFlux/reactive 使用方式冲突，停止并记录适配方案，不要复制一套不兼容逻辑。

# Executor Prompt Contract

实现平台中立的 JWT 认证组件。不要把 todo、iam-center 或 process 的业务 claims 规则硬编码到 starter。
