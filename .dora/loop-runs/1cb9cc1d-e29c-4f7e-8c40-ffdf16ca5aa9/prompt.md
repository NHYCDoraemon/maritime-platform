You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526A-T04`
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

# [Gateway Starter Audit Fix] HMAC nonce 只在验签通过后提交

## 任务概要

调整 HMAC 认证顺序，避免无效请求在验签前消耗 Redis nonce。

## 开发背景

当前 `HmacAuthenticationManager` 在 credential resolve 和 signature verify 之前调用 `nonceValidator.validate(appKey, nonce)`。攻击者可以用正确 bodyDigest 但错误 signature 的请求提前占用 nonce，导致后续合法请求被误判 `REPLAY_DETECTED`。

## 范围

- 先完成 timestamp、nonce 长度、bodyDigest 校验。
- 再解析 credential 并校验 app 可用性。
- 再计算 canonical string 和 signature。
- 最后提交 nonce SETNX。
- credential 缺少 `appSecret` 必须 fail closed，返回明确认证错误。

## 非目标

- 不移除 HMAC nonce 防重放能力。
- 不新增业务 app credential 管理接口。
- 不改变 Redis key 格式，除非测试证明现有格式无法保证安全语义。

## 实现要求

调整 `HmacAuthenticationManager.authenticate()` 的响应式链：基础 header、timestamp、nonce 长度和 bodyDigest 校验通过后，先解析 credential 并检查 secret，再计算 signature，最后调用 `HmacNonceValidator.validate()` 提交 nonce。通过 Mockito `verify(..., never())` 覆盖无效签名、unknown app、disabled app 和缺少 secret 等失败路径。

## 验收标准

- 错误 signature 不调用 nonce validator。
- unknown/disabled app 不调用 nonce validator。
- 缺少 appSecret 不抛 NPE/500。
- 两个相同有效请求仍只有一个通过。

## 验证要求

先运行 HMAC manager/resolver 聚焦测试，再运行完整 starter 测试。

## 停止条件

- 如果调整顺序导致两个并发有效请求都能通过，停止并重新设计原子提交点。
- 如果修复只能通过吞掉 Redis 异常达成，停止；安全路径不能 fail open。

## 执行器提示契约

不要牺牲防重放并发语义。正确顺序是“认证有效性确认后原子提交 nonce”，不是完全移除 nonce SETNX。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526A-T04
external_source: dora-orchestrator
source_hash: d3c4444b62867840d721960417a910ebc517e2642650ece687b8ea8d79ebda96
agent_hint: claude
risk: high
depends_on: []
verification_level: 
  - L1
  - L2
---

<!-- dora:metadata
{"execution_packet_hash": "sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md", "required": true, "sha256": "sha256:3db33de3e158e01716daea77fdc7e857ef656fc8371a0e5c1c3a10e5ab1f0457"}], "source_queries": [], "source_tables": [], "verification_commands": ["mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest,DefaultAppCredentialResolverTest,HmacNonceValidatorTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/.dora/source-bundles/20260526A/MP-GWSTARTER-20260526A-T04/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526A/docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`

Generated slices:
- none
