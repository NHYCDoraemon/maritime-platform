---
task_id: MP-GWSTARTER-20260525B-T04
batch_id: 20260525B
program_prefix: GWSTARTER
sequence: 4
title: "[Gateway Starter Fix] 修复 HMAC timestamp 文档与实现契约不一致"
cycle: Gateway Starter Fixes
module: verification
priority: P2
risk: medium
depends_on: []
source_pages:
  - docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md
source_docs:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-fix-batch.md
source_summaries:
  - docs/superpowers/plans/2026-05-25-platform-gateway-starter-fix-batch.md
verification_level:
  - L1
  - L2
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复 README 写秒级 timestamp、实现按毫秒级 timestamp 校验的不一致。

# Development Context

当前 `HmacAuthenticationManager` 和平台已有 HMAC 工具都按 epoch millis 判断 timestamp，但 README 写的是 Unix 秒。按 README 接入的系统调用方会被 gateway 判定为超时。

# Scope

- README 明确 `X-Timestamp` 使用 Unix epoch 毫秒。
- `HmacAuthenticationManager` JavaDoc 与 README 保持一致。
- 测试锁定 epoch millis 被接受、epoch seconds 被拒绝。

# Non-goals

- 不同时兼容秒和毫秒。
- 不改变签名 canonical string。
- 不改变 timestamp tolerance 默认值。

# Implementation Detail

保留现有实现的毫秒契约，修正文档错误。测试中用当前时间毫秒构造成功请求，用当前时间秒构造失败请求，失败错误码应为 `TIMESTAMP_EXPIRED`。

# Acceptance

- README 不再写 “Unix 时间戳（秒）”。
- HMAC 测试明确覆盖 millis/seconds 行为。
- 完整测试通过。

# Verification

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=HmacAuthenticationManagerTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

# Stop Conditions

- 如果产品决定要兼容秒级 timestamp，停止并重新设计明确的双格式兼容策略；不要在本任务里隐式兼容。

# Executor Prompt Contract

这是契约一致性修复。目标是消除文档误导，并用测试锁定现有毫秒级实现。

