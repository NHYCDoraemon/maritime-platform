You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526B-T02`
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

# [Gateway Starter Post-Audit] 无条件剥离 raw HMAC signature headers

## 任务概要

修复 raw HMAC 签名 header 在 HMAC filter 不存在、JWT-only gateway、或 header 名称自定义后仍可能透传到下游的问题。

## 开发背景

当前 `UntrustedHeaderStripFilter` 为了让 HMAC filter 能读取签名材料，会保留配置化 HMAC header；`HmacAuthenticationGatewayFilter` 在认证成功后移除这些配置化 header。但如果 `hmac.enabled=false`，HMAC filter 不注册；如果 app-key header 自定义为 `X-App-Code`，客户端仍可伪造默认 `X-App-Key`。这些 raw signature header 可能被当作普通 header 透传，与 README 和交付目标不一致。

## 范围

- 下游转发前必须剥离默认 HMAC signature headers：`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。
- 下游转发前必须剥离当前配置的自定义 HMAC signature headers。
- 剥离逻辑必须在 `hmac.enabled=false`、`default-auth-mode=none`、JWT-only、HMAC 成功认证后都成立。
- header 名称比较必须大小写不敏感。
- HMAC 认证前仍要允许 HMAC filter 读取当前配置的签名 header。

## 非目标

- 不把 raw signature header 转成可信上下文。
- 不改变 canonical string 字段。
- 不新增业务 app 权限模型。

## 实现要求

先补过滤链失败测试：

- `hmac.enabled=false` 且请求带默认 HMAC signature headers，下游捕获不到这些 header。
- `jwt.enabled=true`、`hmac.enabled=false` 的 JWT-only 路由，下游捕获不到 raw HMAC signature headers。
- 自定义 app-key header 后，请求同时带自定义 header 和默认 `X-App-Key`，下游不能收到任何 raw signature header。
- 大小写混用的签名 header 同样被剥离。

实现上可引入一个始终注册的 outbound signature header strip 逻辑，或扩展现有 `UntrustedHeaderStripFilter` / `ContextHeaderInjectionFilter` 的职责边界；但必须保持 HMAC filter 在认证阶段可读取配置 header。

## 验收标准

- 下游永远不会收到默认 raw HMAC signature headers。
- 下游永远不会收到配置化 raw HMAC signature headers。
- HMAC 正常认证链路仍可通过，并继续注入 `X-Verified-App-Code`、`X-App-Code`、`X-App-Id`、tenant/app permissions 等可信上下文。
- README 中 header 生命周期描述与测试一致。

## 验证要求

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test`
- `mvn -pl platform-gateway-starter -am test`

## 停止条件

- 如果发现已有下游依赖 raw `X-App-Key` 或签名 header，停止并记录迁移方案；不要继续把 raw signature header 透传。
- 如果剥离逻辑破坏 HMAC filter 读取签名材料，先调整过滤器顺序设计再继续。

## 执行器提示契约

这是 P0 header 安全修复。必须用完整过滤链测试证明 HMAC 认证前可读、认证后或非 HMAC 路径不透传。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526B-T02
external_source: dora-orchestrator
source_hash: d2a5436637199a24e4210526154bc881c81b806fac87abc6aae58e4c3451c7cb
agent_hint: claude
risk: high
depends_on: 
  - MP-GWSTARTER-20260526B-T01
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
{"execution_packet_hash": "sha256:0a4fca24ea03554ee76b80df511c61acc4b072221c073f00e2849c63df7e42d6", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_pages", "path": "platform-gateway-starter/README.md", "required": true, "sha256": "sha256:a1679cb2ca9b067bcd3ed956a61ff7f8bcc7204389d9399c5004551948861559"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}], "source_queries": [], "source_tables": [], "verification_commands": ["java -version", "mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/.dora/source-bundles/20260526B/MP-GWSTARTER-20260526B-T02/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/platform-gateway-starter/README.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md`

Generated slices:
- none
