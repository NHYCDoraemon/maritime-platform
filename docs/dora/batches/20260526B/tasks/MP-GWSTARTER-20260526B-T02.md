---
task_id: MP-GWSTARTER-20260526B-T02
batch_id: 20260526B
program_prefix: GWSTARTER
sequence: 2
title: "[Gateway Starter Post-Audit] 无条件剥离 raw HMAC signature headers"
cycle: Gateway Starter Post-Audit Hardening
module: implementation
priority: P0
risk: high
depends_on:
  - MP-GWSTARTER-20260526B-T01
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
  - mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 raw HMAC 签名 header 在 HMAC filter 不存在、JWT-only gateway、或 header 名称自定义后仍可能透传到下游的问题。

# Development Context

当前 `UntrustedHeaderStripFilter` 为了让 HMAC filter 能读取签名材料，会保留配置化 HMAC header；`HmacAuthenticationGatewayFilter` 在认证成功后移除这些配置化 header。但如果 `hmac.enabled=false`，HMAC filter 不注册；如果 app-key header 自定义为 `X-App-Code`，客户端仍可伪造默认 `X-App-Key`。这些 raw signature header 可能被当作普通 header 透传，与 README 和交付目标不一致。

# Scope

- 下游转发前必须剥离默认 HMAC signature headers：`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。
- 下游转发前必须剥离当前配置的自定义 HMAC signature headers。
- 剥离逻辑必须在 `hmac.enabled=false`、`default-auth-mode=none`、JWT-only、HMAC 成功认证后都成立。
- header 名称比较必须大小写不敏感。
- HMAC 认证前仍要允许 HMAC filter 读取当前配置的签名 header。

# Non-goals

- 不把 raw signature header 转成可信上下文。
- 不改变 canonical string 字段。
- 不新增业务 app 权限模型。

# Implementation Detail

先补过滤链失败测试：

- `hmac.enabled=false` 且请求带默认 HMAC signature headers，下游捕获不到这些 header。
- `jwt.enabled=true`、`hmac.enabled=false` 的 JWT-only 路由，下游捕获不到 raw HMAC signature headers。
- 自定义 app-key header 后，请求同时带自定义 header 和默认 `X-App-Key`，下游不能收到任何 raw signature header。
- 大小写混用的签名 header 同样被剥离。

实现上可引入一个始终注册的 outbound signature header strip 逻辑，或扩展现有 `UntrustedHeaderStripFilter` / `ContextHeaderInjectionFilter` 的职责边界；但必须保持 HMAC filter 在认证阶段可读取配置 header。

# Acceptance

- 下游永远不会收到默认 raw HMAC signature headers。
- 下游永远不会收到配置化 raw HMAC signature headers。
- HMAC 正常认证链路仍可通过，并继续注入 `X-Verified-App-Code`、`X-App-Code`、`X-App-Id`、tenant/app permissions 等可信上下文。
- README 中 header 生命周期描述与测试一致。

# Verification

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest test`
- `mvn -pl platform-gateway-starter -am test`

# Stop Conditions

- 如果发现已有下游依赖 raw `X-App-Key` 或签名 header，停止并记录迁移方案；不要继续把 raw signature header 透传。
- 如果剥离逻辑破坏 HMAC filter 读取签名材料，先调整过滤器顺序设计再继续。

# Executor Prompt Contract

这是 P0 header 安全修复。必须用完整过滤链测试证明 HMAC 认证前可读、认证后或非 HMAC 路径不透传。
