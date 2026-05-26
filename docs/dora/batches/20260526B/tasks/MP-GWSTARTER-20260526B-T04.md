---
task_id: MP-GWSTARTER-20260526B-T04
batch_id: 20260526B
program_prefix: GWSTARTER
sequence: 4
title: "[Gateway Starter Post-Audit] 补齐 HMAC body 缓存边界与 README 契约"
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
  - mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationGatewayFilterTest,HmacAuthenticationManagerTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

为 HMAC body 缓存补上明确大小边界，并修正 README 中 HMAC 校验顺序与实现不一致的问题。

# Development Context

HMAC body digest 校验需要读取请求体。当前实现使用 `DataBufferUtils.join(...)` 聚合请求体，再复制到 byte array 并重新包装给下游。这个逻辑缺少 starter 级别的显式大小边界。另一个文档问题是 README 仍写成 nonce SETNX 在 credential lookup 和 signature 校验前执行，而实现已经把 nonce 提交放在签名校验之后。

# Scope

- 增加 HMAC body 最大缓存大小配置，提供保守默认值。
- 超过大小时返回稳定 401 或 413 错误，错误码和 README 保持一致。
- 保持空 body 和小 body 的 HMAC digest 行为不变。
- README 修正 HMAC 服务端校验顺序：credential、secret、canonical string、signature 通过后才提交 nonce。
- README 说明 body 缓存限制和调参方式。

# Non-goals

- 不改 HMAC canonical string 格式。
- 不引入流式 HMAC 签名协议。
- 不改变 Redis nonce key 结构。

# Implementation Detail

先补失败测试：构造超过阈值的 HMAC 请求体，断言不会继续转发下游，并返回稳定错误；同时验证未超过阈值的请求保持现有行为。

实现可在 `GatewaySecurityProperties.Hmac` 中新增 `maxBodyBytes` 或等价配置，默认值需要适合 gateway starter。读取 body 时必须在聚合过程中或聚合后检查大小，避免无界内存增长。若选择依赖 Spring codec 限制，必须证明当前代码路径会受该限制约束，并在 README 写清楚配置项。

# Acceptance

- 超过 HMAC body 缓存限制的请求不会转发下游。
- 未超过限制的有效 HMAC 请求继续通过。
- README 的 nonce 顺序与实现一致。
- README 描述 body 缓存限制、默认值和配置方式。

# Verification

必须在 Java 17 下运行：

- `java -version`
- `mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationGatewayFilterTest,HmacAuthenticationManagerTest test`
- `mvn -pl platform-gateway-starter -am test`

# Stop Conditions

- 如果框架层已经提供可证明的强制 body 限制，停止新增重复配置，改为补测试和 README 证明。
- 如果新增错误码会影响公开 API，先在 README 记录兼容行为再继续。

# Executor Prompt Contract

先用失败测试固定大 body 行为，再改实现。不要只写 README 声明限制而不提供代码或测试证据。
