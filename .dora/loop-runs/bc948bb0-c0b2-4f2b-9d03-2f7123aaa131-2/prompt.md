You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525B-T02`
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

# [Gateway Starter Fix] 修复 JWT 状态校验未接入自动装配认证链

## 任务概要

修复自动装配路径下 `JwtAuthenticationManager` 没有注入 `JwtStateValidator` 的问题。

## 开发背景

审计发现 `JwtStateValidator` 虽然存在，但 Spring 使用的 `JwtAuthenticationManager` 构造器把 state validator 传成 `null`。因此 session、blacklist、user-enabled 校验在真实 starter 中不会执行。

## 范围

- 调整 `JwtAuthenticationManager` Spring 构造器，使 `JwtStateValidator` 在自动装配链路中实际注入。
- 保留测试用构造器，但不能影响生产自动装配路径。
- 补自动装配集成测试，直接通过 Spring context 获取 manager 验证状态校验。

## 非目标

- 不新增 session 写入或注销能力。
- 不调用 IAM SDK 或业务接口。
- 不改变 Redis key 前缀契约。

## 实现要求

将 Spring 构造器改为接收 `JwtStateValidator`，并通过 `this(properties, claimsMapper, Clock.systemUTC(), stateValidator)` 初始化。集成测试必须验证 auto-wired manager，而不是手工 new manager。

## 验收标准

- valid token + Redis 无 session 返回 `SESSION_EXPIRED`。
- valid token + Redis 有 session 可认证成功。
- blacklist 命中返回 `TOKEN_BLACKLISTED`。
- user disabled 返回 `USER_DISABLED`。
- 上述测试通过自动装配获得的 `JwtAuthenticationManager` 执行。

## 验证要求

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=GatewayStarterIntegrationTest,JwtAuthenticationManagerTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

## 停止条件

- 如果 JWT 开启但 Redis 缺失导致上下文无法启动，需要明确这是安全配置要求，不能静默跳过状态校验。

## 执行器提示契约

这是 P0 安全链路修复。不要只测试 `JwtStateValidator` 本身，必须证明自动装配后的 JWT 认证链实际调用状态校验。

## 系统元数据

---
external_id: MP-GWSTARTER-20260525B-T02
external_source: dora-orchestrator
source_hash: f5058e3529b741f2b9d6dfdaa505a187a6bef08ede4669eadec8dbacffd239e7
agent_hint: codex
risk: high
depends_on: []
verification_level: 
  - L1
  - L2
  - L3
---
