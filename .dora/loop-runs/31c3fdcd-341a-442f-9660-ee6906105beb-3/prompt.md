You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526A-T03`
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

# [Gateway Starter Audit Fix] 补齐可信 header 清理并恢复 TraceId 下游透传

## 任务概要

补齐设计稿中的可信 header 清理项，并确保 gateway 捕获的 traceId 继续透传给下游服务。

## 开发背景

设计稿要求清理 `X-User-Permissions`、`X-Test-Channel`，当前实现未覆盖。当前 `TraceIdGatewayFilter` 捕获 `X-Trace-Id` 并写响应，但随后 `UntrustedHeaderStripFilter` 从请求中移除该 header，未再写回下游，导致下游 `platform-common-web` 不能复用 gateway traceId。

## 范围

- 清理清单补齐 `X-User-Permissions`、`X-Test-Channel`。
- 原始 `X-Trace-Id` 必须先被捕获、规范化，再由 gateway 写回下游请求。
- public path、JWT path、HMAC path 行为一致。
- 更新 README 清理清单和 TraceId 说明。

## 非目标

- 不引入分布式追踪 SDK 或链路采样策略。
- 不修改下游 `platform-common-web` 的 servlet filter 行为。
- 不允许客户端原始 trace header 作为可信上下文直接绕过 gateway。

## 实现要求

在可信头清理列表补齐设计稿遗漏项，并在清理后由 gateway 重新写入 `TraceIdGatewayFilter.TRACE_ID_ATTR` 中的值。测试要覆盖客户端传入 traceId、客户端不传 traceId、public path、JWT path 和 HMAC path，断言下游请求和响应使用同一个 gateway 确认后的 traceId。

## 验收标准

- 伪造权限/测试通道 header 不会到达下游。
- 下游请求携带 gateway 确认后的 `X-Trace-Id`。
- 响应 `X-Trace-Id` 与下游请求一致。

## 验证要求

先运行过滤链聚焦测试，再运行完整 starter 测试。

## 停止条件

- 如果发现清理与重注入顺序会破坏现有 filter order，停止并先调整顺序测试。
- 如果下游 traceId 与响应 traceId 不一致，不能提交该任务。

## 执行器提示契约

不要为了透传 traceId 而信任客户端原始 header。必须先捕获/规范化，再由 gateway 写入。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526A-T03
external_source: dora-orchestrator
source_hash: bd5711743e2c97d14e88b6004818244582cd024cfced40c4a604eaf96bd2d72e
agent_hint: claude
risk: medium
depends_on: []
verification_level: 
  - L1
  - L2
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}], "source_queries": [], "source_tables": [], "verification_commands": ["mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,TrustedHeaderWriterTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/.dora/source-bundles/20260526A/MP-GWSTARTER-20260526A-T03/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`

Generated slices:
- none
