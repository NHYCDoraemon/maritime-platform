---
task_id: MP-GWSTARTER-20260526A-T05
batch_id: 20260526A
program_prefix: GWSTARTER
sequence: 5
title: "[Gateway Starter Audit Fix] 统一 HMAC canonical string 与公开签名契约"
cycle: Gateway Starter Audit Hardening
module: implementation
priority: P1
risk: medium
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
  - mvn -pl platform-gateway-starter -Dtest=HmacCanonicalRequestBuilderTest,HmacAuthenticationManagerTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

统一 HMAC canonical string 的设计稿、代码、README 和测试，避免系统调用方无法按公开文档稳定生成签名。

# Development Context

设计稿定义的是带字段名的 method/path/query/bodyDigest canonical string；当前实现是无字段名 7 行格式；`platform-common-core` 现有 `HmacSignatureValidator` 又是旧的 `systemCode=...&timestamp=...` 格式。这个差异会让调用方按不同文档或 helper 生成互不兼容的签名。

# Scope

- 以设计稿强 canonical string 为准：

```text
appKey={appKey}
method={HTTP_METHOD}
path={rawPath}
query={canonicalQuery}
timestamp={timestamp}
nonce={nonce}
bodyDigest={sha256Hex(body)}
```

- 更新 `HmacCanonicalRequestBuilder`、认证测试、README。
- README 写清 query 排序、URL encoding、timestamp millis、bodyDigest 和 signature hex。
- 根 README 模块表补充 `platform-gateway-starter`。
- 对历史 `HmacSignatureValidator` 明确兼容策略：不静默兼容；如需要则新增显式 legacy 模式或 additive helper。

# Non-goals

- 不在同一个默认模式里同时接受多个不兼容签名格式。
- 不迁移历史 gateway 或历史客户端。
- 不把 IAM SDK 作为 gateway starter 的强依赖。

# Implementation Detail

更新 `HmacCanonicalRequestBuilder` 输出带字段名的 7 行格式，并同步修正 `HmacAuthenticationManagerTest` 的签名 helper。README 给出调用方可直接实现的签名步骤。若保留 `platform-common-core` 旧 helper，需要在文档中明确它不是 gateway starter 默认签名契约；如新增 helper，必须是 additive API，并保留旧 helper 兼容。

# Acceptance

- 设计稿、README、测试中的 canonical string 完全一致。
- 测试中的示例客户端按 README 规则生成签名并认证成功。
- 历史 helper 兼容风险被记录，不再隐式混用。

# Verification

先运行 HMAC canonical/auth 聚焦测试，再运行完整 starter 测试。

# Stop Conditions

- 如果 canonical string 改动会破坏已经提交的 live 任务上下文，停止并新建兼容策略说明。
- 如果无法在 README 中给出可复现签名样例，不能提交该任务。

# Executor Prompt Contract

不要只改 README。签名契约必须由生产代码和测试共同锁定。
