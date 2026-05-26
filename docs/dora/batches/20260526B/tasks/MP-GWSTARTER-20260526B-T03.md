---
task_id: MP-GWSTARTER-20260526B-T03
batch_id: 20260526B
program_prefix: GWSTARTER
sequence: 3
title: "[Gateway Starter Post-Audit] 明确并落地 TraceId 规范化合同"
cycle: Gateway Starter Post-Audit Hardening
module: implementation
priority: P1
risk: medium
depends_on:
  - MP-GWSTARTER-20260526B-T02
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
  - platform-gateway-starter/README.md
source_docs:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md
source_summaries:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md
required_skills:
  - maritime-java-backend-development
  - maritime-platform-governance
  - superpowers:test-driven-development
verification_level:
  - L1
  - L2
  - L3
verification_commands:
  - java -version
  - mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

明确 `X-Trace-Id` 的平台合同，并让实现、测试、README 保持一致。

# Development Context

README 描述客户端 `X-Trace-Id` 会被捕获并规范化/净化后向下游透传；当前实现对非空客户端值原样保留并重新注入。两者语义不一致，会影响接入方对 trace header 安全边界的理解。

# Scope

- 定义 TraceId 可接受字符集、最大长度和 fallback 生成规则。
- 对非法、过长、空白或控制字符 TraceId 生成新的 gateway trace id。
- 对合法客户端 TraceId 保持可追踪性并由 gateway 重新写入下游。
- 同步更新 README。
- 补完整过滤链测试覆盖合法、非法、过长、空白四类输入。

# Non-goals

- 不引入分布式 tracing SDK。
- 不改变日志框架。
- 不把用户、租户、应用身份信息编码到 trace id。

# Implementation Detail

建议采用保守合同：只允许 ASCII 字母、数字、`-`、`_`、`.`，长度 1 到 128。空值、空白、包含控制字符或超过长度时，生成 32 位无短横线 UUID。实现应集中在 `TraceIdGatewayFilter` 或一个包内 helper 中，避免过滤链测试和 README 各写一套规则。

# Acceptance

- 合法客户端 `X-Trace-Id` 被 gateway 捕获、清理原始 header、再写入下游。
- 非法 TraceId 不会原样进入下游。
- 下游始终收到一个非空 TraceId。
- README 明确 TraceId 规则，不再使用模糊的“规范化”描述。

# Verification

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest test`
- `mvn -pl platform-gateway-starter -am test`

# Stop Conditions

- 如果已有公开规范要求兼容更宽的 trace id 字符集，停止并按规范调整规则；不要留下“文档说净化、代码原样透传”的状态。

# Executor Prompt Contract

先写 TraceId 行为测试，再改实现和 README。不要只改文档绕过实现缺口。
