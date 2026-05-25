You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T04`
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
external_id: MP-GWSTARTER-20260525A-T04
external_source: dora-orchestrator
source_hash: a4df66501aad68b6e0db96f439b3d2f7dd866a3307a43e806e3bcd9859399e2f
agent_hint: codex
risk: high
depends_on: 
  - MP-GWSTARTER-20260525A-T03
verification_level: 
  - L1
  - L2
---

# Task Summary

实现 starter 的基础 GlobalFilter 链和顺序约束。

# Development Context

gateway starter 的核心是统一横向链路：TraceId、可信头清理、日志、认证策略、上下文注入。顺序错误会导致客户端伪造 header 或下游拿到错误上下文。

# Scope

- 新增 `TraceIdGatewayFilter`。
- 新增 `UntrustedHeaderStripFilter`。
- 新增 `RequestLogGatewayFilter`。
- 新增 `RouteSecurityPolicyFilter`。
- 新增 `ContextHeaderInjectionFilter`。
- 按设计顺序注册：0 TraceId、5 header strip、10 log、20 security、30 context injection。

# Non-goals

- 不在本任务实现 JWT/HMAC 完整认证。
- 不引入 Sentinel/Knife4j。
- 不记录敏感 token 或 signature。

# Implementation Detail

认证结果通过 exchange attribute 传递。`UntrustedHeaderStripFilter` 必须对所有路径执行，包括 public path。

# Acceptance

- 所有路径都会清理可信 header。
- public path 也会清理 `X-Internal-Call` 等可信头。
- filter order 有测试保护。
- 空认证实现下 gateway 可以正常转发 public path。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 测试可信头清理和 filter order。

# Stop Conditions

- 如果现有平台已经有相同 GlobalFilter order 常量，停止并复用或对齐已有常量。

# Executor Prompt Contract

优先保证链路骨架和安全顺序正确。不要把业务日志字段或项目特定 header 写进 starter。
