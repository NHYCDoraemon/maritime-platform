---
task_id: MP-GWSTARTER-20260526A-T02
batch_id: 20260526A
program_prefix: GWSTARTER
sequence: 2
title: "[Gateway Starter Audit Fix] HMAC 签名 header 生命周期与可配置 header 支持"
cycle: Gateway Starter Audit Hardening
module: implementation
priority: P0
risk: high
depends_on: []
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md
source_summaries:
  - docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md
verification_level:
  - L1
  - L2
  - L3
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest,GatewayStarterIntegrationTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 HMAC 签名 header 认证前保留、认证后清理的生命周期，并覆盖自定义 HMAC header 名称。

# Development Context

20260525B 已修复默认 `X-App-Key` 被预认证清理的问题，但实现仍使用静态清理清单。若调用方配置 `maritime.gateway.security.hmac.headers.app-key=X-App-Code`，该 header 会与下游应用上下文 header 冲突。另一个缺口是 `JWT_OR_HMAC` 走 JWT 分支或 public path 时，原始 HMAC 签名 header 可能透传下游。

# Scope

- 让 header strip 逻辑读取 `GatewaySecurityProperties.Hmac.Headers`。
- 认证前保留当前配置的 HMAC 签名 header。
- 认证决策后，所有转发路径都移除原始 HMAC 签名 header。
- 保持上下文 header 由 `ContextHeaderInjectionFilter` 重新注入。
- 更新 README 对签名 header 和上下文 header 的说明。

# Non-goals

- 不改变 HMAC canonical string 或签名算法。
- 不把原始签名 header 作为可信上下文透传给业务服务。
- 不新增业务 app 权限解析逻辑。

# Implementation Detail

将 `UntrustedHeaderStripFilter` 改为构造器注入 `GatewaySecurityProperties`，清理时排除当前配置的 HMAC 入站签名 header。`HmacAuthenticationGatewayFilter` 成功认证后继续移除签名 header，并补充 JWT、NONE、public path、`JWT_OR_HMAC` 走 JWT 分支等旁路场景的清理测试，确保原始签名材料不会到达下游。

# Acceptance

- 默认 `X-App-Key` 可认证。
- 自定义 `X-App-Code` 作为 app-key header 可认证。
- 下游不会收到原始 HMAC 签名 header。
- HMAC 成功路径仍能收到 verified app context。

# Verification

先运行过滤链和 HMAC filter 聚焦测试，再运行完整 starter 测试。

# Stop Conditions

- 如果自定义 app-key header 与下游上下文 header 同名时无法同时满足认证前可读和认证后不透传，停止并记录兼容设计。
- 如果修复需要修改业务服务消费约定，停止并另起消费者迁移批次。

# Executor Prompt Contract

不要把签名 header 当作可信上下文 header 直接透传。签名 header 只允许在 gateway 认证内部使用。
