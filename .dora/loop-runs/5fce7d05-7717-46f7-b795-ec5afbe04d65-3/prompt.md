You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526B-T03`
- **Batch**: `20260526B`
- **Branch / cwd**: `orchestrator/20260526B` (you are already inside the worktree at `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B`)

# Required Skills

Load and use these skills before starting engineering work:
- REQUIRED SKILL: maritime-java-backend-development
- REQUIRED SKILL: maritime-platform-governance
- REQUIRED SKILL: superpowers:test-driven-development

If this skill is unavailable, stop and report the missing skill.
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

# [Gateway Starter Post-Audit] 明确并落地 TraceId 规范化合同

## 任务概要

明确 `X-Trace-Id` 的平台合同，并让实现、测试、README 保持一致。

## 开发背景

README 描述客户端 `X-Trace-Id` 会被捕获并规范化/净化后向下游透传；当前实现对非空客户端值原样保留并重新注入。两者语义不一致，会影响接入方对 trace header 安全边界的理解。

## 范围

- 定义 TraceId 可接受字符集、最大长度和 fallback 生成规则。
- 对非法、过长、空白或控制字符 TraceId 生成新的 gateway trace id。
- 对合法客户端 TraceId 保持可追踪性并由 gateway 重新写入下游。
- 同步更新 README。
- 补完整过滤链测试覆盖合法、非法、过长、空白四类输入。

## 非目标

- 不引入分布式 tracing SDK。
- 不改变日志框架。
- 不把用户、租户、应用身份信息编码到 trace id。

## 实现要求

建议采用保守合同：只允许 ASCII 字母、数字、`-`、`_`、`.`，长度 1 到 128。空值、空白、包含控制字符或超过长度时，生成 32 位无短横线 UUID。实现应集中在 `TraceIdGatewayFilter` 或一个包内 helper 中，避免过滤链测试和 README 各写一套规则。

## 验收标准

- 合法客户端 `X-Trace-Id` 被 gateway 捕获、清理原始 header、再写入下游。
- 非法 TraceId 不会原样进入下游。
- 下游始终收到一个非空 TraceId。
- README 明确 TraceId 规则，不再使用模糊的“规范化”描述。

## 验证要求

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest test`
- `mvn -pl platform-gateway-starter -am test`

## 停止条件

- 如果已有公开规范要求兼容更宽的 trace id 字符集，停止并按规范调整规则；不要留下“文档说净化、代码原样透传”的状态。

## 执行器提示契约

先写 TraceId 行为测试，再改实现和 README。不要只改文档绕过实现缺口。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526B-T03
external_source: dora-orchestrator
source_hash: e6382fe3233bd88894267c3ab1ec8ed6ecdf56b186eb6cd209a56466f24d2aa6
agent_hint: claude
risk: medium
depends_on: 
  - MP-GWSTARTER-20260526B-T02
required_skills:
  - maritime-java-backend-development
  - maritime-platform-governance
  - superpowers:test-driven-development
verification_level: 
  - L1
  - L2
  - L3
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:0a4fca24ea03554ee76b80df511c61acc4b072221c073f00e2849c63df7e42d6", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_pages", "path": "platform-gateway-starter/README.md", "required": true, "sha256": "sha256:a1679cb2ca9b067bcd3ed956a61ff7f8bcc7204389d9399c5004551948861559"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}], "source_queries": [], "source_tables": [], "verification_commands": ["java -version", "mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/.dora/source-bundles/20260526B/MP-GWSTARTER-20260526B-T03/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/platform-gateway-starter/README.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md`

Generated slices:
- none
