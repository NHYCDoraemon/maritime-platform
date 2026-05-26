You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526B-T04`
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

# [Gateway Starter Post-Audit] 补齐 HMAC body 缓存边界与 README 契约

## 任务概要

为 HMAC body 缓存补上明确大小边界，并修正 README 中 HMAC 校验顺序与实现不一致的问题。

## 开发背景

HMAC body digest 校验需要读取请求体。当前实现使用 `DataBufferUtils.join(...)` 聚合请求体，再复制到 byte array 并重新包装给下游。这个逻辑缺少 starter 级别的显式大小边界。另一个文档问题是 README 仍写成 nonce SETNX 在 credential lookup 和 signature 校验前执行，而实现已经把 nonce 提交放在签名校验之后。

## 范围

- 增加 HMAC body 最大缓存大小配置，提供保守默认值。
- 超过大小时返回稳定 401 或 413 错误，错误码和 README 保持一致。
- 保持空 body 和小 body 的 HMAC digest 行为不变。
- README 修正 HMAC 服务端校验顺序：credential、secret、canonical string、signature 通过后才提交 nonce。
- README 说明 body 缓存限制和调参方式。

## 非目标

- 不改 HMAC canonical string 格式。
- 不引入流式 HMAC 签名协议。
- 不改变 Redis nonce key 结构。

## 实现要求

先补失败测试：构造超过阈值的 HMAC 请求体，断言不会继续转发下游，并返回稳定错误；同时验证未超过阈值的请求保持现有行为。

实现可在 `GatewaySecurityProperties.Hmac` 中新增 `maxBodyBytes` 或等价配置，默认值需要适合 gateway starter。读取 body 时必须在聚合过程中或聚合后检查大小，避免无界内存增长。若选择依赖 Spring codec 限制，必须证明当前代码路径会受该限制约束，并在 README 写清楚配置项。

## 验收标准

- 超过 HMAC body 缓存限制的请求不会转发下游。
- 未超过限制的有效 HMAC 请求继续通过。
- README 的 nonce 顺序与实现一致。
- README 描述 body 缓存限制、默认值和配置方式。

## 验证要求

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationGatewayFilterTest,HmacAuthenticationManagerTest test`
- `mvn -pl platform-gateway-starter -am test`

## 停止条件

- 如果框架层已经提供可证明的强制 body 限制，停止新增重复配置，改为补测试和 README 证明。
- 如果新增错误码会影响公开 API，先在 README 记录兼容行为再继续。

## 执行器提示契约

先用失败测试固定大 body 行为，再改实现。不要只写 README 声明限制而不提供代码或测试证据。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526B-T04
external_source: dora-orchestrator
source_hash: 20564fde558f61f28aa60ba02a2c053df7c1b028a8427b44db6f87a9d5edb56c
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
{"execution_packet_hash": "sha256:0a4fca24ea03554ee76b80df511c61acc4b072221c073f00e2849c63df7e42d6", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_pages", "path": "platform-gateway-starter/README.md", "required": true, "sha256": "sha256:a1679cb2ca9b067bcd3ed956a61ff7f8bcc7204389d9399c5004551948861559"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}], "source_queries": [], "source_tables": [], "verification_commands": ["java -version", "mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationGatewayFilterTest,HmacAuthenticationManagerTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/.dora/source-bundles/20260526B/MP-GWSTARTER-20260526B-T04/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/platform-gateway-starter/README.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md`

Generated slices:
- none
