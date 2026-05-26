# Gateway Starter Post-Audit Hardening Batch

Date: 2026-05-26
Owner: platform
Scope: `platform-gateway-starter`

## 审计结论

当前 `platform-gateway-starter` 在 Java 17 下测试通过，但仍不能按“新项目引入依赖 + 配置即可安全复用 gateway”的交付标准关闭。剩余问题不是编译或普通单测问题，而是安全默认值、扩展点一致性和公开契约的缺口。

已验证：

- `java -version` 使用 Temurin OpenJDK 17。
- `mvn -pl platform-gateway-starter -Dtest=GatewaySecurityPropertiesTest,GatewayAutoConfigurationTest,GatewayFilterChainTest,HmacAuthenticationGatewayFilterTest,HmacAuthenticationManagerTest,GatewaySentinelAutoConfigurationTest test` 通过，134 tests。
- `mvn -pl platform-gateway-starter -am test` 通过，377 tests。

测试通过只能说明当前覆盖面内行为稳定；下面缺口仍会影响交付判断。

## 交付阻断项

### 1. Programmatic route policy 绕过 fail-closed

`GatewaySecurityProperties.afterPropertiesSet()` 只校验默认配置和 properties routes。`RouteSecurityPolicyResolver.addRoutePolicy()` 作为公开扩展点，可以在 `jwt.enabled=false` 或 `hmac.enabled=false` 时添加 `JWT`、`HMAC`、`JWT_OR_HMAC` 路由。对应认证过滤器由 `@ConditionalOnProperty` 控制，未启用时不会注册，最终可能出现 route policy 是认证模式但请求仍被转发。

交付标准要求所有会产生认证要求的入口统一 fail closed，包括配置文件和代码扩展点。

### 2. Raw HMAC signature headers 仍可能透传下游

当前 header 生命周期修复覆盖了 HMAC 过滤器存在且使用当前配置 header 的路径。但如果 `hmac.enabled=false`，或者 app-key header 被自定义后客户端继续伪造默认 `X-App-Key`，这些原始签名 header 不一定会被统一剥离。

交付标准要求下游只接收 gateway 生成或验证后的可信上下文，不接收任何原始签名材料。

## 设计与文档不一致项

### 3. README 中 HMAC nonce 顺序与实现不一致

实现已经把 nonce 提交放在 credential lookup、secret 校验、canonical string 和 signature 校验之后，这是正确方向。但 README 的服务端校验流程仍写成先 SETNX nonce，再查 credential 和验签。公开文档会误导接入方和安全审计。

### 4. TraceId “规范化/净化”契约未落地

实现保留非空客户端 `X-Trace-Id` 原值并重新注入下游。README 描述为捕获并规范化/净化。需要明确 TraceId 合同：要么实现长度、字符集、fallback 生成等规范化；要么把文档改成“保留客户端值但由 gateway 重新写入”。从安全和可观测性角度，建议实现明确规范化。

### 5. HMAC body 缓存缺少显式大小边界

HMAC body digest 需要读取 body，但当前 `DataBufferUtils.join(...)` 没有 starter 级别的显式大小限制或文档化依赖。作为平台 starter，需要给大请求内存风险一个明确策略。

## 新批次目标

本批次只处理后审计发现的剩余交付缺口：

- 统一配置 routes 与 programmatic route policy 的认证模式校验。
- 无条件剥离 raw HMAC signature headers，包括默认 header 和自定义 header。
- 明确并落地 TraceId 规范化合同。
- 给 HMAC body 缓存补上大小边界或明确的框架级限制接入。
- 修正文档，使 README 与实现一致。

## 非目标

- 不引入业务权限、数据权限或历史项目特殊路由。
- 不实现 `JWT_AND_HMAC` 双认证链路。
- 不改变已验证的 HMAC canonical string 公开格式，除非测试证明 README 与代码仍存在矛盾。
- 不把原始 HMAC 签名 header 当作可信上下文继续传递。

## 全局验收

- Java 17 下 `mvn -pl platform-gateway-starter -am test` 通过。
- 错误认证配置无论来自 properties 还是 `GatewaySecurityPolicyCustomizer` 都启动失败。
- 下游不会收到 raw HMAC signature headers。
- TraceId 行为和 README 完全一致，并有过滤链测试覆盖。
- HMAC body 缓存策略有可执行测试或明确文档约束。
