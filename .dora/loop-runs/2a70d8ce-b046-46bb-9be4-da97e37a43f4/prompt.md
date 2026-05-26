You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526A-T01`
- **Batch**: `20260526A`
- **Branch / cwd**: `orchestrator/20260526A` (you are already inside the worktree at `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A`)

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

# [Gateway Starter Audit Fix] auth-mode 与启用开关 fail-closed

## 任务概要

修复认证模式需要 JWT/HMAC，但对应 `jwt.enabled` / `hmac.enabled` 未启用时过滤器不注册、请求静默放行的问题。

## 开发背景

当前 `JwtAuthenticationGatewayFilter` 和 `HmacAuthenticationGatewayFilter` 都由 `@ConditionalOnProperty` 控制。若配置解析出 `AuthMode.JWT` 或 `AuthMode.HMAC`，但对应 enabled 为 false，`RouteSecurityPolicyFilter` 只写入 policy，不会阻止请求。

## 范围

- 启动期校验 default auth mode 与 route auth mode。
- `JWT` 要求 JWT enabled。
- `HMAC` 要求 HMAC enabled。
- `JWT_OR_HMAC` 要求 JWT 与 HMAC 都 enabled。
- `NONE` 保持可用。
- `JWT_AND_HMAC` 继续启动期拒绝。

## 非目标

- 不实现 `JWT_AND_HMAC` 的双认证链路。
- 不改变 route path/method 匹配算法。
- 不新增业务权限、数据权限或历史 gateway 特判。

## 实现要求

在 `GatewaySecurityProperties.afterPropertiesSet()` 中集中检查所有会产生认证要求的配置项。默认认证模式和每个 route policy 都必须与对应 enabled 开关一致；错误信息要包含 default 或 route id，方便新项目启动期定位配置问题。同步补充自动装配测试，证明错误配置不会进入可转发状态。

## 验收标准

- 错误组合启动失败且错误信息说明具体 route/default auth mode。
- 显式 `default-auth-mode=none` 可启动。
- 自动装配测试证明不存在认证过滤器缺失却继续转发的配置。

## 验证要求

先运行聚焦测试，再运行完整 starter 测试。

## 停止条件

- 如果发现已有 README 推荐的最小配置会被新校验误伤，先更新文档和测试再继续。
- 如果某些历史消费者依赖 `default-auth-mode=jwt` 但未显式启用 JWT，停止并记录兼容迁移方案。

## 执行器提示契约

必须先补失败回归测试。不要通过默认注册空认证过滤器来“通过测试”；安全目标是错误配置 fail closed。

## 系统元数据

---
dora_retry_count: 3
external_id: MP-GWSTARTER-20260526A-T01
external_source: dora-orchestrator
source_hash: 3b2aa00c9c55cfbb1c265d3ba2024e2107f0d5589ec9a4b14692be50f721be9d
agent_hint: claude
risk: high
depends_on: []
verification_level: 
  - L1
  - L2
  - L3
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}], "source_queries": [], "source_tables": [], "verification_commands": ["mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/.dora/source-bundles/20260526A/MP-GWSTARTER-20260526A-T01/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`

Generated slices:
- none
