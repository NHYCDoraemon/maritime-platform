# Program: platform-gateway-starter-post-audit-hardening

## Intent

把 `platform-gateway-starter` 从“测试全绿”推进到“可交付复用”。本计划聚焦安全默认值、扩展点一致性、header 生命周期、TraceId 合同和大请求资源边界。

## Audit Conclusion

当前实现未完全达成目标。主要原因是部分安全校验只覆盖 properties 配置，未覆盖公开编程扩展点；HMAC 签名 header 清理依赖 HMAC 过滤器存在和当前配置 header，仍有透传路径；README 与实现存在流程描述不一致；TraceId 的“净化/规范化”没有明确合同；HMAC body 缓存缺少显式边界。

## Platform Boundary

本批仍限定在平台 gateway 横向能力，不加入业务域权限、数据权限、资源树注册、业务路由适配或历史项目特殊规则。

## Acceptance

- Java 17 下完整 starter 测试通过。
- 所有认证模式入口都 fail closed。
- 下游只收到 gateway 验证或生成后的可信上下文 header。
- README 与代码行为一致。
- 资源边界和错误行为可被测试或文档证明。
