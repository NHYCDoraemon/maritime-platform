---
batch_id: 20260525B
program_id: platform-gateway-starter-fixes
program_prefix: GWSTARTER
title: 平台网关 Starter 审计定向修复
status: approved
approved_by: raymond
approved_at: 2026-05-25T16:32:43+08:00
approved_batch_hash: sha256:24ae197ae56825650b1afb383338c5315d75c88bff572731aa02db10c74c7af3
approved_task_count: 4
---

# Batch Summary

本批只修复 `platform-gateway-starter` 审计中发现的 4 个根因问题，不扩大 gateway starter 能力范围。

目标是让 starter 真正满足“新项目引入依赖 + 配置即可安全复用 gateway”的开发目标：

- HMAC 请求不能被预认证 header 清理破坏。
- JWT session/blacklist/user-enabled 必须在自动装配链路中实际生效。
- route 配置漏写 `auth-mode` 必须 fail closed，不能静默放行。
- HMAC timestamp 对外文档必须和实现保持一致。

# Source

- Design: `docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`
- Original implementation plan: `docs/superpowers/plans/2026-05-25-platform-gateway-starter-implementation-plan.md`
- Fix plan: `docs/superpowers/plans/2026-05-25-platform-gateway-starter-fix-batch.md`
