---
batch_id: 20260526B
program_id: platform-gateway-starter-post-audit-hardening
program_prefix: GWSTARTER
title: 平台网关 Starter 后审计交付硬化
submitted_by: codex
submitted_at: 2026-05-26T10:52:21+08:00
planned_task_count: 4
status: approved
approved_by: raymond
approved_at: 2026-05-26T10:52:21+08:00
approved_batch_hash: sha256:0a4fca24ea03554ee76b80df511c61acc4b072221c073f00e2849c63df7e42d6
approved_task_count: 4
---

# Batch Summary

本批来自 2026-05-26 对 `platform-gateway-starter` 的后审计。结论是：当前实现已经在 Java 17 下测试全绿，但还不能按交付标准关闭，因为仍存在扩展点绕过 fail-closed、raw HMAC 签名 header 透传、TraceId 合同不明确、HMAC body 缓存缺少边界等问题。

本批只修复交付阻断和高价值硬化项：

- programmatic route policy 必须与 properties route 一样执行 auth-mode 与 enabled 开关一致性校验。
- raw HMAC signature headers 必须在任何下游转发路径中剥离。
- TraceId 合同必须明确并由代码和测试落地。
- HMAC body 缓存必须有明确大小边界或受控框架配置。

# Source

- Design: `docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- README: `platform-gateway-starter/README.md`
- Post-audit plan: `docs/superpowers/plans/2026-05-26-platform-gateway-starter-post-audit-hardening-batch.md`
