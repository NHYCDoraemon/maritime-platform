# Program: platform-gateway-starter-audit-hardening

## Intent

修复 gateway starter 完整性审计中发现的剩余安全与契约缺口，确保 starter 不只是组件测试通过，而是在真实新项目 gateway 自动装配场景下 fail closed、可配置、可验证。

## Audit Conclusion

当前实现未完全达成目标。`mvn -pl platform-gateway-starter -am test` 已通过，但测试覆盖没有触达以下关键行为：

- 认证模式需要的组件未启用时，过滤器缺失会造成静默放行。
- HMAC 签名 header 的配置化能力与静态可信头清理冲突。
- 下游仍可能收到原始 HMAC 签名 header。
- 设计稿要求清理的部分可信 header 未覆盖。
- gateway 捕获 traceId 后没有向下游透传。
- HMAC nonce 在验签前写入 Redis，错误请求可消耗 nonce。
- HMAC canonical string 在设计稿、代码、README 之间不完全一致。
- Sentinel block handler 使用 `Map.of(..., null)`，真实执行会失败。

## Platform Boundary

本批仍限定在平台 gateway 横向能力，不加入业务路由、业务权限、数据权限、资源树注册或历史项目特殊规则。

## Acceptance

- `mvn -pl platform-gateway-starter -am test` 通过。
- 错误认证配置启动失败，不会出现缺少认证过滤器却转发请求的路径。
- 下游只收到 gateway 验证或生成后的上下文 header 和 traceId。
- HMAC nonce 只在完整认证成功后提交。
- HMAC 签名算法有唯一公开契约。
- Sentinel optional path 有运行时 handler 验证。

