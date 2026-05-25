You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260525B-T01`
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

# [Gateway Starter Fix] 修复 HMAC 签名 header 被预认证清理破坏

## 任务概要

修复 `UntrustedHeaderStripFilter` 在 HMAC 认证前移除 `X-App-Key`，导致系统请求无法认证的问题。

## 开发背景

审计发现过滤器顺序是 header strip 先于 HMAC auth。当前清理清单包含 `X-App-Key`，而 HMAC auth 需要从 `X-App-Key` 读取 appKey，因此真实 HMAC 请求会被错误判定为 `MISSING_HMAC_HEADERS`。

## 范围

- 区分“入站 HMAC 签名 header”和“下游可信上下文 header”。
- 预认证清理阶段保留 HMAC 签名所需 header。
- HMAC 认证成功后，转发下游前移除原始签名 header。
- 补完整过滤链回归测试。

## 非目标

- 不改变 HMAC canonical string。
- 不新增业务 app 权限逻辑。
- 不改变 JWT 路径的 nonce header 处理。

## 实现要求

修改 `UntrustedHeaderStripFilter`，不要在预认证阶段移除配置默认 HMAC 签名 header：`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。保留对下游上下文 header 的清理：`X-App-Code`、`X-App-Id`、`X-Verified-App-Code`、`X-App-Permissions`、tenant/user/internal headers。

修改 `HmacAuthenticationGatewayFilter`，认证成功后基于配置移除原始 HMAC 签名 header，再让 `ContextHeaderInjectionFilter` 注入可信应用上下文。

## 验收标准

- 完整过滤链下 HMAC 请求不会因为 `X-App-Key` 被清理而失败。
- HMAC 认证成功后，下游不会收到原始签名 header。
- 下游仍能收到 `X-Verified-App-Code`、`X-App-Code`、`X-App-Id`、`X-Tenant-Code`、`X-Tenant-Id`、`X-App-Permissions`。

## 验证要求

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

## 停止条件

- 如果发现下游已有服务依赖原始 `X-App-Key`，停止并记录兼容方案；不要直接把原始签名 header 作为可信上下文继续透传。

## 执行器提示契约

这是 P0 安全链路修复。必须先补失败回归测试，再改代码。不要把 HMAC 入站签名 header 加入 trusted context。

## 系统元数据

---
external_id: MP-GWSTARTER-20260525B-T01
external_source: dora-orchestrator
source_hash: 34a4cd765493e02d60e936ef0360e53f3639f717cac1b2a6338e7368239d5080
agent_hint: codex
risk: high
depends_on: []
verification_level: 
  - L1
  - L2
  - L3
---
