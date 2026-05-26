You are running unattended inside the dora orchestrator. There is no human in the loop on this run; act with full decision authority.

# Task to execute now
- **Plane Issue**: `MP-GWSTARTER-20260526B-T01`
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

# [Gateway Starter Post-Audit] 补齐 programmatic route policy fail-closed

## 任务概要

修复 `GatewaySecurityPolicyCustomizer` / `RouteSecurityPolicyResolver.addRoutePolicy()` 添加认证路由时绕过 fail-closed 校验的问题。

## 开发背景

`GatewaySecurityProperties.afterPropertiesSet()` 已经校验 default auth mode 和 properties routes，但 programmatic routes 在 `RouteSecurityPolicyResolver` 中编译，当前只拒绝 `JWT_AND_HMAC`，没有校验 `jwt.enabled`、`hmac.enabled`。如果 programmatic route 要求 `JWT` 或 `HMAC`，但对应过滤器未注册，请求可能带着认证 policy 继续转发。

## 范围

- 抽出或复用统一的 auth-mode 与 enabled 开关一致性校验逻辑。
- 覆盖 `default-auth-mode`、properties routes、programmatic routes。
- `JWT` 要求 `jwt.enabled=true`。
- `HMAC` 要求 `hmac.enabled=true`。
- `JWT_OR_HMAC` 要求 JWT 与 HMAC 都启用。
- `NONE` 保持可用。
- `JWT_AND_HMAC` 继续启动期拒绝。

## 非目标

- 不实现 `JWT_AND_HMAC` 双认证。
- 不改变 route path/method 匹配优先级。
- 不为禁用的认证模式注册空过滤器。

## 实现要求

先补失败回归测试：用 `ApplicationContextRunner` 注册 `GatewaySecurityPolicyCustomizer`，分别添加 `JWT`、`HMAC`、`JWT_OR_HMAC` programmatic route，同时保持对应 enabled 开关为 false，断言 Spring context 启动失败且错误信息包含 route id 和 auth mode。

实现时建议把当前 `GatewaySecurityProperties` 内部的模式一致性校验提取成包内可复用组件，或由 `RouteSecurityPolicyResolver` 在 customizers 执行后统一校验 `programmaticRoutes`。错误信息必须能定位来源，至少包含 `programmatic route`、route id、auth mode 和缺失的 enabled 开关。

## 验收标准

- programmatic `JWT` route 且 `jwt.enabled=false` 时启动失败。
- programmatic `HMAC` route 且 `hmac.enabled=false` 时启动失败。
- programmatic `JWT_OR_HMAC` route 只启用一个认证组件时启动失败。
- programmatic `NONE` route 仍可启动。
- properties route 和 default auth mode 的既有 fail-closed 测试继续通过。

## 验证要求

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test`
- `mvn -pl platform-gateway-starter -am test`

## 停止条件

- 如果发现 programmatic route 设计上允许在运行期动态切换 enabled 开关，停止并记录兼容方案；不要把认证缺失路径放行。
- 如果现有测试依赖未启用认证组件的 programmatic `JWT` 或 `HMAC` route，需要先修正测试语义，再改实现。

## 执行器提示契约

这是 P0 安全修复。必须先写失败测试，再改实现。不要用空认证过滤器、默认放行或降低 route policy 的方式掩盖问题。

## 系统元数据

---
external_id: MP-GWSTARTER-20260526B-T01
external_source: dora-orchestrator
source_hash: eaa203a2737a3201aab5372067d3b6d652fb6dac6c1893a5274915d2f34a4cee
agent_hint: claude
risk: high
depends_on: []
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
{"execution_packet_hash": "sha256:0a4fca24ea03554ee76b80df511c61acc4b072221c073f00e2849c63df7e42d6", "execution_packet_version": 1, "source_docs": [{"kind": "source_pages", "path": "docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md", "required": true, "sha256": "sha256:0633485dc1f0421dfc63725f6f6a773db9d30f679f4c1ca8ca73e6ce9a721361"}, {"kind": "source_pages", "path": "platform-gateway-starter/README.md", "required": true, "sha256": "sha256:a1679cb2ca9b067bcd3ed956a61ff7f8bcc7204389d9399c5004551948861559"}, {"kind": "source_docs", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}, {"kind": "source_summaries", "path": "docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md", "required": true, "sha256": "sha256:c6723138fa8aa5746bfa3237a4745c24538309d8299c6b04d3849db8f668294d"}], "source_queries": [], "source_tables": [], "verification_commands": ["java -version", "mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayStarterIntegrationTest test", "mvn -pl platform-gateway-starter -am test"]}
dora:metadata -->

## Source Context Contract

Before editing, read every required path below. Treat these files as the source-of-truth context for this task.

Required reads:
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/.dora/source-bundles/20260526B/MP-GWSTARTER-20260526B-T01/source-bundle.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/platform-gateway-starter/README.md`
- `/Users/raymond/.dora/orchestrator/worktrees/maritime-platform/20260526B/docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md`

Generated slices:
- none
