# Program: platform-gateway-starter-fixes

## Intent

修复平台 gateway starter 审计发现的安全链路阻断问题。这个 batch 不是继续拆大功能，也不是补泛化建设任务；每个 issue 都对应一个已确认缺陷和一个可验证回归测试。

## Platform Boundary

这些修复仍属于平台横向能力：

- HMAC 入站签名认证。
- JWT 状态校验。
- route 级认证策略配置。
- HMAC 调用契约文档。

不加入 todo、IAM、process 的业务语义，不设计迁移落地。

## Acceptance

- `mvn -pl platform-gateway-starter -am test` 通过。
- 自动装配后的 JWT 认证链实际执行 session、blacklist、user-enabled。
- 完整过滤链下 HMAC 请求可以完成认证，且下游只收到认证后注入的可信上下文 header。
- 错误 route 配置启动失败。
- README 与代码测试共同约束 HMAC timestamp 使用 epoch millis。

