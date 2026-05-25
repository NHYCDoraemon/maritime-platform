---
task_id: MP-GWSTARTER-20260526A-T04
batch_id: 20260526A
program_prefix: GWSTARTER
sequence: 4
title: "[Gateway Starter Audit Fix] HMAC nonce 只在验签通过后提交"
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
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest,DefaultAppCredentialResolverTest,HmacNonceValidatorTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

调整 HMAC 认证顺序，避免无效请求在验签前消耗 Redis nonce。

# Development Context

当前 `HmacAuthenticationManager` 在 credential resolve 和 signature verify 之前调用 `nonceValidator.validate(appKey, nonce)`。攻击者可以用正确 bodyDigest 但错误 signature 的请求提前占用 nonce，导致后续合法请求被误判 `REPLAY_DETECTED`。

# Scope

- 先完成 timestamp、nonce 长度、bodyDigest 校验。
- 再解析 credential 并校验 app 可用性。
- 再计算 canonical string 和 signature。
- 最后提交 nonce SETNX。
- credential 缺少 `appSecret` 必须 fail closed，返回明确认证错误。

# Non-goals

- 不移除 HMAC nonce 防重放能力。
- 不新增业务 app credential 管理接口。
- 不改变 Redis key 格式，除非测试证明现有格式无法保证安全语义。

# Implementation Detail

调整 `HmacAuthenticationManager.authenticate()` 的响应式链：基础 header、timestamp、nonce 长度和 bodyDigest 校验通过后，先解析 credential 并检查 secret，再计算 signature，最后调用 `HmacNonceValidator.validate()` 提交 nonce。通过 Mockito `verify(..., never())` 覆盖无效签名、unknown app、disabled app 和缺少 secret 等失败路径。

# Acceptance

- 错误 signature 不调用 nonce validator。
- unknown/disabled app 不调用 nonce validator。
- 缺少 appSecret 不抛 NPE/500。
- 两个相同有效请求仍只有一个通过。

# Verification

先运行 HMAC manager/resolver 聚焦测试，再运行完整 starter 测试。

# Stop Conditions

- 如果调整顺序导致两个并发有效请求都能通过，停止并重新设计原子提交点。
- 如果修复只能通过吞掉 Redis 异常达成，停止；安全路径不能 fail open。

# Executor Prompt Contract

不要牺牲防重放并发语义。正确顺序是“认证有效性确认后原子提交 nonce”，不是完全移除 nonce SETNX。
