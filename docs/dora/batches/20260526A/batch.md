---
batch_id: 20260526A
program_id: platform-gateway-starter-audit-hardening
program_prefix: GWSTARTER
title: 平台网关 Starter 完整性审计修复
submitted_by: codex
submitted_at: 2026-05-26T00:08:32+08:00
planned_task_count: 6
status: approved
approved_by: raymond
approved_at: 2026-05-26T00:16:22+08:00
approved_batch_hash: sha256:6577ae6082027c87bb30e883f6a0ea7bb96bee0afebea52982ff8fc7d8e9efa2
approved_task_count: 6
---

# Batch Summary

本批来自 2026-05-26 对 `platform-gateway-starter` 完整实现的审计。结论是：现有实现已经完成主要功能并且当前测试全绿，但尚未完全达成“新项目引入依赖 + 配置即可安全复用 gateway”的目标。

本批只修复审计确认的横向能力缺口：

- auth-mode 与 `jwt.enabled` / `hmac.enabled` 不一致时必须 fail closed。
- HMAC 签名 header 必须支持配置化，认证前可读，认证后不透传。
- 可信 header 清理必须覆盖设计清单，TraceId 必须透传给下游。
- HMAC nonce 不能被未认证请求提前消耗。
- HMAC canonical string 必须与公开设计和 README 一致。
- Sentinel block handler 必须在运行时可返回稳定 JSON。

# Source

- Design: `docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- Original implementation plan: `docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md`
- First fix batch: `docs/superpowers/plans/2026-05-25-platform-gateway-starter-fix-batch.md`
- Audit hardening plan: `docs/superpowers/plans/2026-05-26-platform-gateway-starter-audit-hardening-batch.md`
