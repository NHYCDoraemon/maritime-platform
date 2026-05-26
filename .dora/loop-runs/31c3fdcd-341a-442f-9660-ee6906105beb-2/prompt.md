You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526A-T02`
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

# [Gateway Starter Audit Fix] HMAC 签名 header 生命周期与可配置 header 支持

## 任务概要

修复 HMAC 签名 header 认证前保留、认证后清理的生命周期，并覆盖自定义 HMAC header 名称。

## 开发背景

20260525B 已修复默认 `X-App-Key` 被预认证清理的问题，但实现仍使用静态清理清单。若调用方配置 `maritime.gateway.security.hmac.headers.app-key=X-App-Code`，该 header 会与下游应用上下文 header 冲突。另一个缺口是 `JWT_OR_HMAC` 走 JWT 分支或 public path 时，原始 HMAC 签名 header 可能透传下游。

## 范围

- 让 header strip 逻辑读取 `GatewaySecurityProperties.Hmac.Headers`。
- 认证前保留当前配置的 HMAC 签名 header。
- 认证决策后，所有转发路径都移除原始 HMAC 签名 header。
- 保持上下文 header 由 `ContextHeaderInjectionFilter` 重新注入。
- 更新 README 对签名 header 和上下文 header 的说明。

## 非目标

- 不改变 HMAC canonical string 或签名算法。
- 不把原始签名 header 作为可信上下文透传给业务服务。
- 不新增业务 app 权限解析逻辑。

## 实现要求

将 `UntrustedHeaderStripFilter` 改为构造器注入 `GatewaySecurityProperties`，清理时排除当前配置的 HMAC 入站签名 header。`HmacAuthenticationGatewayFilter` 成功认证后继续移除签名 header，并补充 JWT、NONE、public path、`JWT_OR_HMAC` 走 JWT 分支等旁路场景的清理测试，确保原始签名材料不会到达下游。

## 验收标准

- 默认 `X-App-Key` 可认证。
- 自定义 `X-App-Code` 作为 app-key header 可认证。
- 下游不会收到原始 HMAC 签名 header。
- HMAC 成功路径仍能收到 verified app context。

## 验证要求

先运行过滤链和 HMAC filter 聚焦测试，再运行完整 starter 测试。

## 停止条件

- 如果自定义 app-key header 与下游上下文 header 同名时无法同时满足认证前可读和认证后不透传，停止并记录兼容设计。
- 如果修复需要修改业务服务消费约定，停止并另起消费者迁移批次。

## 执行器提示契约

不要把签名 header 当作可信上下文 header 直接透传。签名 header 只允许在 gateway 认证内部使用。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526A-T02
external_source: dora-orchestrator
source_hash: d3cf002c4e604cc0c984562a13cd3b1d1e7e5ff252477713517c20ab34fd421f
agent_hint: claude
risk: high
depends_on: []
verification_level: 
  - L1
  - L2
  - L3
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}], "source_queries": [], "source_tables": [], "verification_commands": ["mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest,GatewayStarterIntegrationTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/.dora/source-bundles/20260526A/MP-GWSTARTER-20260526A-T02/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`

Generated slices:
- none
