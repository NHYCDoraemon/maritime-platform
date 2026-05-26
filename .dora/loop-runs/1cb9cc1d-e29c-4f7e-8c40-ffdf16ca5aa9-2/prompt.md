You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526A-T05`
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

# [Gateway Starter Audit Fix] 统一 HMAC canonical string 与公开签名契约

## 任务概要

统一 HMAC canonical string 的设计稿、代码、README 和测试，避免系统调用方无法按公开文档稳定生成签名。

## 开发背景

设计稿定义的是带字段名的 method/path/query/bodyDigest canonical string；当前实现是无字段名 7 行格式；`platform-common-core` 现有 `HmacSignatureValidator` 又是旧的 `systemCode=...&timestamp=...` 格式。这个差异会让调用方按不同文档或 helper 生成互不兼容的签名。

## 范围

- 以设计稿强 canonical string 为准：

```text
appKey={appKey}
method={HTTP_METHOD}
path={rawPath}
query={canonicalQuery}
timestamp={timestamp}
nonce={nonce}
bodyDigest={sha256Hex(body)}
```

- 更新 `HmacCanonicalRequestBuilder`、认证测试、README。
- README 写清 query 排序、URL encoding、timestamp millis、bodyDigest 和 signature hex。
- 根 README 模块表补充 `platform-gateway-starter`。
- 对历史 `HmacSignatureValidator` 明确兼容策略：不静默兼容；如需要则新增显式 legacy 模式或 additive helper。

## 非目标

- 不在同一个默认模式里同时接受多个不兼容签名格式。
- 不迁移历史 gateway 或历史客户端。
- 不把 IAM SDK 作为 gateway starter 的强依赖。

## 实现要求

更新 `HmacCanonicalRequestBuilder` 输出带字段名的 7 行格式，并同步修正 `HmacAuthenticationManagerTest` 的签名 helper。README 给出调用方可直接实现的签名步骤。若保留 `platform-common-core` 旧 helper，需要在文档中明确它不是 gateway starter 默认签名契约；如新增 helper，必须是 additive API，并保留旧 helper 兼容。

## 验收标准

- 设计稿、README、测试中的 canonical string 完全一致。
- 测试中的示例客户端按 README 规则生成签名并认证成功。
- 历史 helper 兼容风险被记录，不再隐式混用。

## 验证要求

先运行 HMAC canonical/auth 聚焦测试，再运行完整 starter 测试。

## 停止条件

- 如果 canonical string 改动会破坏已经提交的 live 任务上下文，停止并新建兼容策略说明。
- 如果无法在 README 中给出可复现签名样例，不能提交该任务。

## 执行器提示契约

不要只改 README。签名契约必须由生产代码和测试共同锁定。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526A-T05
external_source: dora-orchestrator
source_hash: d9562ea9a86c1ec58cd4920e59c216fba76ddd202e29e06d3f8e5fa12fee133a
agent_hint: claude
risk: medium
depends_on: []
verification_level: 
  - L1
  - L2
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}], "source_queries": [], "source_tables": [], "verification_commands": ["mvn -pl platform-gateway-starter -Dtest=HmacCanonicalRequestBuilderTest,HmacAuthenticationManagerTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/.dora/source-bundles/20260526A/MP-GWSTARTER-20260526A-T05/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`

Generated slices:
- none
