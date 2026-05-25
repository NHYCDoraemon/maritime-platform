You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525A-T03`
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
external_id: MP-GWSTARTER-20260525A-T03
external_source: dora-orchestrator
source_hash: b586d608b24e1bff5add5e437c049b8cbc4f3e3de6e9dae5bfe315ccfc99f89a
agent_hint: codex
risk: medium
depends_on: 
  - MP-GWSTARTER-20260525A-T02
verification_level: 
  - L1
  - L2
---

# Task Summary

实现按路径和 method 解析认证模式的 `RouteSecurityPolicyResolver`。

# Development Context

新项目 gateway 不能再写自定义认证过滤器，必须通过配置表达哪些路径公开、哪些路径使用 JWT、HMAC 或混合模式。

# Scope

- 新增 `RouteSecurityPolicyResolver`。
- public paths 优先匹配为 `none`。
- route policy 支持 paths/methods 匹配。
- 未命中 route policy 时使用 `default-auth-mode`。
- 提供 `GatewaySecurityPolicyCustomizer` 扩展点。

# Non-goals

- 不解析业务权限。
- 不实现 JWT/HMAC 本身。
- 不维护动态路由表。

# Implementation Detail

优先使用 Spring Gateway/WebFlux 生态中稳定的 path matcher，避免手写字符串匹配。解析结果应是内部 policy 对象，供后续 filter 使用。

# Acceptance

- public path 一律解析为 `none`。
- route policy 能覆盖默认认证模式。
- 支持同一路径不同 method 配不同策略。
- 未命中 route policy 时回落默认模式。

# Verification

- 执行 `mvn -pl platform-gateway-starter -am test`。
- 单元测试覆盖匹配优先级、method 过滤、默认回退和 customizer。

# Stop Conditions

- 如果 path matcher 与 Gateway 实际路由语义明显不一致，停止并改用 Gateway 原生匹配机制。

# Executor Prompt Contract

保证策略解析是纯平台能力，不引入项目业务路径。测试应明确保护 public path 优先级。
