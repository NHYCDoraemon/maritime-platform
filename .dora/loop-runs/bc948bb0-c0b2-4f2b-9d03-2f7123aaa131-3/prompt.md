You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525B-T03`
- **Batch**: `20260525B`
- **Branch / cwd**: `orchestrator/20260525B` (you are already inside the worktree at `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform`)

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

# [Gateway Starter Fix] 修复 route auth-mode 缺失导致静默放行

## 任务概要

修复 route policy 配置了 paths 但漏写 `auth-mode` 时静默放行的问题。

## 开发背景

审计发现 `RoutePolicy.authMode` 可为 null。resolver 会把 null 策略放进 exchange，JWT/HMAC filter 发现不是自己的模式后直接放行。这是配置型安全绕过。

## 范围

- `RoutePolicy.authMode` 增加非空约束。
- `GatewaySecurityProperties.afterPropertiesSet()` 对 route auth-mode 做显式 fail-closed 校验。
- `RouteSecurityPolicyResolver.addRoutePolicy(..., null)` 显式拒绝。
- 补配置绑定和 resolver 回归测试。

## 非目标

- 不新增业务权限策略。
- 不改变 `public-paths` 优先级。
- 不把漏配 route 自动回落 default auth mode。

## 实现要求

错误配置必须启动失败，而不是 fallback。启动失败信息需要包含 route id 和 `auth-mode`，方便新项目快速定位配置问题。

## 验收标准

- route 缺少 `auth-mode` 时启动期失败。
- programmatic route policy 传 null authMode 时抛 `IllegalArgumentException`。
- public paths 仍解析为 `NONE`。
- 未命中 route 仍使用 `default-auth-mode`。

## 验证要求

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,RouteSecurityPolicyResolverTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

## 停止条件

- 如果有既有配置依赖 route auth-mode 为空来表达默认认证，停止并记录迁移说明；不能保留静默放行。

## 执行器提示契约

这是 P1 fail-closed 修复。不要把 null auth-mode 解释成 `NONE`，也不要自动当作 default auth mode。

## 系统元数据

---
external_id: MP-GWSTARTER-20260525B-T03
external_source: dora-orchestrator
source_hash: 623f5e661eba644c6a8b0489c097ae88fac829dfda8c7b0143dd82993230ca4b
agent_hint: codex
risk: high
depends_on: []
verification_level: 
  - L1
  - L2
  - L3
---
