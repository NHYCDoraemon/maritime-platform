# Program: platform-gateway-starter

## Intent

建设统一、可复用的 `platform-gateway-starter`，替代每个新系统重复开发 gateway 安全链路的做法。

## Security Baseline

- 用户请求：JWT + nonce/session/blacklist/user-enabled。
- 系统对系统请求：HMAC + timestamp/nonce/bodyDigest。
- 高安全代理/代办请求：预留 `jwt-and-hmac`，第一版不默认开放。
- 所有路径先清理可信 header，再注入 starter 验证后的上下文 header。
- 租户、用户、应用权限的业务消费继续由下游服务手动引入 `iam-sdk` 或业务 SDK 完成。

## Non-goals

- 不实现历史 gateway 迁移。
- 不把 todo、IAM admin、process admin confirm 等业务规则放入平台 starter。
- 不强制 gateway 引入当前偏 MVC/Servlet 的 `iam-sdk` 自动配置。

## Delivery Shape

新项目 gateway 只保留启动类、依赖和配置。认证过滤器、HMAC 验签、防重放、状态校验、上下文注入、错误响应、日志和 trace 等通用能力由 starter 提供。

