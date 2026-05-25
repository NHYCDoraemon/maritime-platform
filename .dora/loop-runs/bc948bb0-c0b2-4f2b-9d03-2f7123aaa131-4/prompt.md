You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525B-T04`
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

# [Gateway Starter Fix] 修复 HMAC timestamp 文档与实现契约不一致

## 任务概要

修复 README 写秒级 timestamp、实现按毫秒级 timestamp 校验的不一致。

## 开发背景

当前 `HmacAuthenticationManager` 和平台已有 HMAC 工具都按 epoch millis 判断 timestamp，但 README 写的是 Unix 秒。按 README 接入的系统调用方会被 gateway 判定为超时。

## 范围

- README 明确 `X-Timestamp` 使用 Unix epoch 毫秒。
- `HmacAuthenticationManager` JavaDoc 与 README 保持一致。
- 测试锁定 epoch millis 被接受、epoch seconds 被拒绝。

## 非目标

- 不同时兼容秒和毫秒。
- 不改变签名 canonical string。
- 不改变 timestamp tolerance 默认值。

## 实现要求

保留现有实现的毫秒契约，修正文档错误。测试中用当前时间毫秒构造成功请求，用当前时间秒构造失败请求，失败错误码应为 `TIMESTAMP_EXPIRED`。

## 验收标准

- README 不再写 “Unix 时间戳（秒）”。
- HMAC 测试明确覆盖 millis/seconds 行为。
- 完整测试通过。

## 验证要求

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

## 停止条件

- 如果产品决定要兼容秒级 timestamp，停止并重新设计明确的双格式兼容策略；不要在本任务里隐式兼容。

## 执行器提示契约

这是契约一致性修复。目标是消除文档误导，并用测试锁定现有毫秒级实现。

## 系统元数据

---
external_id: MP-GWSTARTER-20260525B-T04
external_source: dora-orchestrator
source_hash: 75128004f71867610ac9d3058ca3424afd09b109dce03b3a39fcb7f351515eb1
agent_hint: codex
risk: medium
depends_on: []
verification_level: 
  - L1
  - L2
---
