You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526A-T06`
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

# [Gateway Starter Audit Fix] 修复 Sentinel block handler 运行时响应

## 任务概要

修复 Sentinel Gateway block handler 在真实执行时因 `Map.of(..., null)` 抛出异常的问题。

## 开发背景

`GatewaySentinelAutoConfiguration` 注册的 block handler 使用 `Map.of("code", 429, "message", "FLOW_LIMITING", "data", null)` 构造响应体。Java `Map.of` 不允许 null value，真实限流响应会抛出运行时异常。现有测试只验证 bean 激活，没有执行 handler。

## 范围

- 替换不允许 null 的 Map 构造方式。
- 增加 handler 执行测试，断言 429 和 JSON body。
- 保持 Sentinel 依赖缺失或开关关闭时不激活。

## 非目标

- 不实现 Sentinel 规则管理 UI。
- 不改变未启用 Sentinel 时的 starter 自动装配行为。
- 不引入业务限流策略或业务路由语义。

## 实现要求

替换 `GatewaySentinelAutoConfiguration` 中的 `Map.of(..., null)`，使用允许 null 值的可序列化响应结构，或直接构造 JSON 响应。测试不仅要断言 bean 激活，还要调用 `GatewayCallbackManager` 注册的 block handler，读取响应状态和 body，确认真实限流路径不会抛出异常。

## 验收标准

- block handler 执行不抛 NPE。
- 响应体包含 `code=429`、`message=FLOW_LIMITING`、`data=null`。
- optional dependency 条件路径不变。

## 验证要求

先运行 Sentinel 聚焦测试，再运行完整 starter 测试。

## 停止条件

- 如果 handler 执行测试需要真实 Sentinel 网关上下文而当前测试环境无法构造，先记录最小可替代集成验证方案。
- 如果修复会激活未启用 Sentinel 的自动装配路径，停止并修正条件注解。

## 执行器提示契约

不要把 `data` 字段直接删掉绕过测试；错误响应契约需要保持与 gateway 默认错误响应一致。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526A-T06
external_source: dora-orchestrator
source_hash: 73d274433c74de0079ada67db8ba58ebb252694ff7775a44d025dcf18ad23ed2
agent_hint: claude
risk: medium
depends_on: []
verification_level: 
  - L1
  - L2
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}], "source_queries": [], "source_tables": [], "verification_commands": ["mvn -pl platform-gateway-starter -Dtest=GatewaySentinelAutoConfigurationTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/.dora/source-bundles/20260526A/MP-GWSTARTER-20260526A-T06/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`

Generated slices:
- none
