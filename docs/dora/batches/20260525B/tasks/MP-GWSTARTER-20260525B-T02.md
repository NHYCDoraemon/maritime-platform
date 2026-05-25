---
task_id: MP-GWSTARTER-20260525B-T02
batch_id: 20260525B
program_prefix: GWSTARTER
sequence: 2
title: "[Gateway Starter Fix] 修复 JWT 状态校验未接入自动装配认证链"
cycle: Gateway Starter Fixes
module: implementation
priority: P0
risk: high
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
  - L3
verification_commands:
  - mvn -pl platform-gateway-starter -Dtest=GatewayStarterIntegrationTest,JwtAuthenticationManagerTest test
  - mvn -pl platform-gateway-starter -am test
---

# Task Summary

修复自动装配路径下 `JwtAuthenticationManager` 没有注入 `JwtStateValidator` 的问题。

# Development Context

审计发现 `JwtStateValidator` 虽然存在，但 Spring 使用的 `JwtAuthenticationManager` 构造器把 state validator 传成 `null`。因此 session、blacklist、user-enabled 校验在真实 starter 中不会执行。

# Scope

- 调整 `JwtAuthenticationManager` Spring 构造器，使 `JwtStateValidator` 在自动装配链路中实际注入。
- 保留测试用构造器，但不能影响生产自动装配路径。
- 补自动装配集成测试，直接通过 Spring context 获取 manager 验证状态校验。

# Non-goals

- 不新增 session 写入或注销能力。
- 不调用 IAM SDK 或业务接口。
- 不改变 Redis key 前缀契约。

# Implementation Detail

将 Spring 构造器改为接收 `JwtStateValidator`，并通过 `this(properties, claimsMapper, Clock.systemUTC(), stateValidator)` 初始化。集成测试必须验证 auto-wired manager，而不是手工 new manager。

# Acceptance

- valid token + Redis 无 session 返回 `SESSION_EXPIRED`。
- valid token + Redis 有 session 可认证成功。
- blacklist 命中返回 `TOKEN_BLACKLISTED`。
- user disabled 返回 `USER_DISABLED`。
- 上述测试通过自动装配获得的 `JwtAuthenticationManager` 执行。

# Verification

- 先运行聚焦测试：`mvn -pl platform-gateway-starter -Dtest=GatewayStarterIntegrationTest,JwtAuthenticationManagerTest test`。
- 再运行完整测试：`mvn -pl platform-gateway-starter -am test`。

# Stop Conditions

- 如果 JWT 开启但 Redis 缺失导致上下文无法启动，需要明确这是安全配置要求，不能静默跳过状态校验。

# Executor Prompt Contract

这是 P0 安全链路修复。不要只测试 `JwtStateValidator` 本身，必须证明自动装配后的 JWT 认证链实际调用状态校验。

