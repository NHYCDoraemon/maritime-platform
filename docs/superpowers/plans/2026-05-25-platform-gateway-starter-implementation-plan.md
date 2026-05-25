# Platform Gateway Starter Implementation Plan

> Spec: `docs/superpowers/specs/2026-05-25-platform-gateway-starter-design.md`

## Scope

本计划把统一 `platform-gateway-starter` 设计拆成可实际落地的开发任务。目标是交付一个公用、独立、可复用的网关 starter，让新项目只通过引入依赖和配置即可获得标准 gateway 能力。

本计划不覆盖 `todo-center`、`iam-center`、`process-engine` 的迁移落地；这些项目仅作为行为参考和兼容验证样本。

## Task 1: 新增 platform-gateway-starter 模块骨架

目标：建立 starter 基础模块、依赖管理和自动装配入口。

开发内容：

- 在根 `pom.xml` 增加 `platform-gateway-starter` module。
- 在 `platform-bom/pom.xml` 管理 starter 版本。
- 新建 `platform-gateway-starter/pom.xml`，引入 Spring Cloud Gateway、Spring Boot autoconfigure、Reactive Redis、Actuator 相关依赖。
- 增加 `GatewayAutoConfiguration` 和 `AutoConfiguration.imports`。
- 增加基础包结构：
  - `com.maritime.platform.gateway.autoconfigure`
  - `com.maritime.platform.gateway.config`
  - `com.maritime.platform.gateway.filter`
  - `com.maritime.platform.gateway.security`
  - `com.maritime.platform.gateway.security.jwt`
  - `com.maritime.platform.gateway.security.hmac`
  - `com.maritime.platform.gateway.security.nonce`
  - `com.maritime.platform.gateway.header`
  - `com.maritime.platform.gateway.error`

验收标准：

- `mvn -pl platform-gateway-starter -am test` 可以执行。
- 新模块被根工程和 BOM 正确纳入。
- 引入 starter 的 Spring Boot Gateway 应用可以完成自动配置扫描。

## Task 2: 实现网关安全配置模型

目标：提供 `maritime.gateway.security.*` 的统一配置入口。

开发内容：

- 新增 `GatewaySecurityProperties`。
- 实现认证模式枚举：`none`、`jwt`、`hmac`、`jwt-or-hmac`，预留 `jwt-and-hmac`。
- 实现 public paths、default auth mode、route policies 配置。
- 实现 JWT 配置：encrypted、secret、issuer、clock skew、claims mapping、Redis keys、validation、nonce。
- 实现 HMAC 配置：timestamp tolerance、nonce、headers、credential source、Redis fields、config fallback apps。
- 对配置增加校验，避免启用 JWT/HMAC 但缺少必要参数。

验收标准：

- 配置类可以被 `@ConfigurationProperties` 正确绑定。
- 缺省配置可启动。
- 错误配置能在启动期给出清晰异常。
- `jwt-and-hmac` 可被识别但第一版不默认开放。

## Task 3: 实现 route 级认证策略解析

目标：让不同路径通过配置选择 `none/jwt/hmac/jwt-or-hmac`。

开发内容：

- 新增 `RouteSecurityPolicyResolver`。
- 支持 public paths 优先匹配。
- 支持 route policy 按 paths/methods 匹配。
- 未命中 route policy 时使用 `default-auth-mode`。
- 提供 `GatewaySecurityPolicyCustomizer` 扩展点。

验收标准：

- public path 一律解析为 `none`。
- route policy 能覆盖默认认证模式。
- 支持同一路径多 method 不同策略。
- 单元测试覆盖匹配优先级和未命中行为。

## Task 4: 实现基础 GlobalFilter 链和执行顺序

目标：建立 gateway 横向过滤链，统一 TraceId、可信头清理、日志、策略认证和上下文注入的执行顺序。

开发内容：

- 新增 `TraceIdGatewayFilter`。
- 新增 `UntrustedHeaderStripFilter`。
- 新增 `RequestLogGatewayFilter`。
- 新增 `RouteSecurityPolicyFilter`。
- 新增 `ContextHeaderInjectionFilter`。
- 按设计顺序注册：
  - `HIGHEST_PRECEDENCE + 0` TraceId
  - `HIGHEST_PRECEDENCE + 5` trusted header strip
  - `HIGHEST_PRECEDENCE + 10` request log
  - `HIGHEST_PRECEDENCE + 20` security policy
  - `HIGHEST_PRECEDENCE + 30` context header injection

验收标准：

- 所有路径都会清理可信 header，包括 public paths。
- 认证结果通过 exchange attribute 传递。
- 过滤器顺序有单元测试保护。

## Task 5: 实现 JWT 用户认证链

目标：完成用户请求标准链路：JWT 提取、解密/验签、claims 映射。

开发内容：

- 新增 `JwtAuthenticationManager`。
- 复用或适配平台现有 JWT/JWT 加解密能力。
- 支持 `Authorization: Bearer <token>` 提取。
- 支持 encrypted JWT 配置。
- 校验签名、issuer、exp、clock skew。
- 新增 `JwtClaimsMapper` 扩展点。
- 输出统一 `GatewayPrincipal.User`。

验收标准：

- 缺失 token 返回 `MISSING_TOKEN`。
- 过期或非法 token 返回对应错误。
- claims 中缺少 userId/sessionId 时认证失败。
- 成功后 exchange attribute 中存在用户 principal。

## Task 6: 实现 JWT session、blacklist、user-enabled 状态校验

目标：对用户 JWT 增加平台统一状态校验。

开发内容：

- 新增 `JwtStateValidator`。
- 使用 Reactive Redis 校验：
  - `iam:session:{sessionId}`
  - `iam:token:blacklist:{jti}`
  - `iam:user:enabled:{userId}`
- 支持 `require-session`、`check-blacklist`、`check-user-enabled` 开关。
- 支持 `user-enabled-disabled-value` 配置。

验收标准：

- session 不存在返回 `SESSION_EXPIRED`。
- blacklist 命中返回 `TOKEN_BLACKLISTED`。
- user disabled 返回 `USER_DISABLED`。
- 状态校验开关可以独立关闭。

## Task 7: 实现 JWT nonce 防重放

目标：为用户写请求提供基于 nonce 的防重放能力。

开发内容：

- 新增 `JwtNonceValidator`。
- 默认实现 `simple-setnx` 模式。
- 默认只对 `POST,PUT,PATCH,DELETE` 要求 `X-Nonce`。
- Redis key 格式：`platform:gateway:jwt:nonce:{sessionId}:{nonce}`。
- 支持 TTL 配置，默认 5 分钟。

验收标准：

- 写请求缺少 nonce 返回 `NONCE_REQUIRED`。
- 重复 nonce 返回 `REPLAY_DETECTED`。
- TTL 过期后 nonce 可重新使用。
- GET 等未配置 method 默认不要求 nonce。

## Task 8: 实现 HMAC 系统请求认证链

目标：完成系统对系统请求标准链路：timestamp、bodyDigest、nonce、signature。

开发内容：

- 新增 `HmacAuthenticationManager`。
- 新增 `HmacCanonicalRequestBuilder`。
- 读取标准 headers：`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`。
- 校验 timestamp 窗口和 nonce 长度。
- 使用 Redis SETNX 防重放，key 为 `platform:gateway:hmac:nonce:{appKey}:{nonce}`。
- 按标准 canonical string 计算 HMAC-SHA256。
- 使用 constant-time compare 比对签名。
- 输出统一 `GatewayPrincipal.App`。

验收标准：

- 缺少 HMAC header 返回 `MISSING_HMAC_HEADERS`。
- timestamp 超窗返回 `TIMESTAMP_EXPIRED`。
- nonce 重复返回 `REPLAY_DETECTED`。
- bodyDigest 不一致或签名不一致返回 `INVALID_SIGNATURE`。
- 成功后 exchange attribute 中存在应用 principal。

## Task 9: 实现 AppCredentialResolver

目标：为 HMAC 提供统一 app 凭证解析能力。

开发内容：

- 新增 `AppCredentialResolver` 接口和默认实现。
- 默认读取 Redis hash：`iam:app:auth:{appKey}`。
- 支持字段映射：
  - `appSecret`
  - `appCode`
  - `appId`
  - `tenantId`
  - `tenantCode`
  - `permissions`
  - `isEnabled`
- 支持配置 fallback apps。
- 支持自定义 bean 覆盖默认 resolver。

验收标准：

- Redis 中不存在 appKey 返回 `UNKNOWN_APP`。
- disabled app 返回 `APP_DISABLED`。
- Redis 不存在时可使用配置 fallback。
- 自定义 `AppCredentialResolver` bean 可覆盖默认实现。

## Task 10: 实现上下文 header 注入和扩展点

目标：将认证后的可信用户/应用上下文注入下游请求。

开发内容：

- 新增 `GatewayPrincipal` 模型。
- 新增 `TrustedHeaderWriter`。
- 新增 `GatewayPrincipalHeaderCustomizer` 扩展点。
- JWT 成功后注入：
  - `X-User-Id`
  - `X-User-Name`
  - `X-Active-Org-Code`
  - `X-Active-Org-Name`
  - `X-Tenant-Id`
  - `X-Session-Id`
  - `X-System-Scope`
  - `X-User-Source`
- HMAC 成功后注入：
  - `X-Verified-App-Code`
  - `X-App-Code`
  - `X-App-Id`
  - `X-Tenant-Code`
  - `X-Tenant-Id`
  - `X-App-Permissions`

验收标准：

- 客户端伪造的可信 header 会先被清理。
- 下游只收到 starter 认证后注入的 header。
- JWT 与 HMAC 注入字段互不污染。
- 自定义 header customizer 可追加项目字段。

## Task 11: 实现统一错误响应

目标：提供 gateway 认证失败时的一致 JSON 响应。

开发内容：

- 新增 `GatewayErrorWriter` 默认实现。
- 响应格式：`{"code":401,"message":"MISSING_TOKEN","data":null}`。
- 响应头包含 `X-Trace-Id`。
- 覆盖错误码：
  - `MISSING_TOKEN`
  - `INVALID_TOKEN`
  - `TOKEN_EXPIRED`
  - `SESSION_EXPIRED`
  - `TOKEN_BLACKLISTED`
  - `USER_DISABLED`
  - `NONCE_REQUIRED`
  - `NONCE_INVALID`
  - `MISSING_HMAC_HEADERS`
  - `TIMESTAMP_EXPIRED`
  - `REPLAY_DETECTED`
  - `UNKNOWN_APP`
  - `APP_DISABLED`
  - `INVALID_SIGNATURE`
  - `FORBIDDEN`

验收标准：

- 401/403 状态码与错误码语义一致。
- 所有认证失败路径都通过统一 writer 输出。
- 自定义 `GatewayErrorWriter` bean 可覆盖默认实现。

## Task 12: 实现 jwt-or-hmac 模式和 jwt-and-hmac 预留行为

目标：支持同一路径同时接受用户请求或系统请求，并为未来高安全代理请求留下稳定接口。

开发内容：

- 在 `RouteSecurityPolicyFilter` 中实现 `jwt-or-hmac`：
  - 有 Bearer token 时走 JWT。
  - 没有 Bearer token 时走 HMAC。
- 对 `jwt-and-hmac` 增加显式保留行为：
  - 可被配置模型识别。
  - 第一版默认返回明确错误或启动期拒绝业务使用。
- 补充模式语义测试。

验收标准：

- `jwt-or-hmac` 下 JWT 成功时注入用户上下文。
- `jwt-or-hmac` 下无 Bearer token 时可通过 HMAC。
- `jwt-and-hmac` 不会被误当作已完成能力开放。

## Task 13: 补充 starter 文档和新项目接入样例

目标：让新项目无需阅读实现即可接入 gateway starter。

开发内容：

- 新增 starter README。
- 提供最小 gateway 示例 `application.yml`。
- 说明用户请求 JWT + nonce/session/blacklist 标准。
- 说明系统请求 HMAC + timestamp/nonce 标准。
- 说明下游服务如需租户、用户、应用权限，应手动引入 `iam-sdk` 或业务 SDK。
- 说明第一版不包含 todo/IAM/process 业务特殊规则。

验收标准：

- README 中包含最小可运行配置。
- README 明确 starter 与 `iam-sdk` 的关系。
- README 明确哪些 header 会被清理和注入。

## Task 14: 补充单元测试和 Gateway 集成测试

目标：用测试锁定 starter 的可复用行为。

开发内容：

- Route policy 匹配测试。
- JWT claims、state、nonce 测试。
- HMAC canonical string、timestamp、nonce、signature 测试。
- Trusted header strip 测试。
- Context header injection 测试。
- 使用 Spring Cloud Gateway 测试请求流。
- 使用 Testcontainers Redis 验证 nonce、session、blacklist、app credential。
- 验证 `none/jwt/hmac/jwt-or-hmac` 四种模式。
- 验证缺省配置可启动。

验收标准：

- starter 单元测试和集成测试通过。
- 关键安全链路都有失败和成功分支覆盖。
- 新项目只引入 starter + 配置即可跑通测试 gateway。

## Task 15: 可选集成 Sentinel Gateway 与 Knife4j Gateway 配置

目标：把常见 gateway 横向能力以可选配置方式纳入 starter。

开发内容：

- 评估并加入 Sentinel Gateway block handler 自动配置。
- 评估并加入 Knife4j/OpenAPI 聚合相关配置辅助。
- 配置默认关闭或条件启用，避免影响最小 starter 使用。

验收标准：

- 未引入 Sentinel/Knife4j 依赖时 starter 不报错。
- 引入依赖并开启配置后可以自动生效。
- 不引入业务路由语义。

## Execution Order

推荐执行顺序：

1. Task 1 到 Task 4：先建立模块、配置、策略和过滤链骨架。
2. Task 5 到 Task 7：完成用户请求 JWT 标准链。
3. Task 8 到 Task 9：完成系统请求 HMAC 标准链。
4. Task 10 到 Task 12：补齐上下文、错误响应和混合模式。
5. Task 13 到 Task 15：完成文档、测试和可选集成。

## Plane Issue Mapping

每个 Task 可作为一个 Plane issue。建议 issue title 使用：

- `[Gateway Starter] 新增 platform-gateway-starter 模块骨架`
- `[Gateway Starter] 实现网关安全配置模型`
- `[Gateway Starter] 实现 route 级认证策略解析`
- `[Gateway Starter] 实现基础 GlobalFilter 链和执行顺序`
- `[Gateway Starter] 实现 JWT 用户认证链`
- `[Gateway Starter] 实现 JWT session/blacklist/user-enabled 状态校验`
- `[Gateway Starter] 实现 JWT nonce 防重放`
- `[Gateway Starter] 实现 HMAC 系统请求认证链`
- `[Gateway Starter] 实现 AppCredentialResolver`
- `[Gateway Starter] 实现上下文 header 注入和扩展点`
- `[Gateway Starter] 实现统一错误响应`
- `[Gateway Starter] 实现 jwt-or-hmac 模式和 jwt-and-hmac 预留行为`
- `[Gateway Starter] 补充 starter 文档和新项目接入样例`
- `[Gateway Starter] 补充单元测试和 Gateway 集成测试`
- `[Gateway Starter] 可选集成 Sentinel Gateway 与 Knife4j Gateway 配置`
