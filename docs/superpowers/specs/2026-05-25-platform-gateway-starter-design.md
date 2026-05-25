# Platform Gateway Starter Design

## 背景

`todo-center` 和 `iam-center` 都已经有独立 gateway，后续新项目也会需要相同类型的网关能力。当前做法的问题不是缺少某一个过滤器，而是每个系统都要重复开发 JWT、HMAC、防重放、可信头清理、上下文注入、TraceId、日志、限流和文档聚合等横向逻辑。

本设计目标是在 `maritime-platform` 中抽象一个统一的工具型平台 starter，让新项目创建 gateway 时只保留极薄模块：启动类、依赖、配置。新项目不需要再编写 gateway 认证过滤器。

## 目标

- 提供单一 `platform-gateway-starter`，新项目直接引入并通过配置启用。
- 标准支持用户请求、系统请求和未来高安全代理请求。
- 统一 JWT、HMAC、nonce、session、blacklist、user-enabled 校验逻辑。
- 统一可信 header 清理和认证后上下文 header 注入。
- 统一 TraceId、请求日志、错误响应等网关基础横向能力。
- 保持平台 API 域中立，不放入 todo、process、IAM 管理端等业务规则。

## 非目标

- 不设计现有 `todo-gateway`、`iam-gateway`、未来 `process-gateway` 的迁移步骤。
- 不把 todo 灰度、IAM admin 特判、process admin confirm 等业务语义放入平台 starter。
- 不要求 gateway 引入当前偏 MVC/Servlet 的 `iam-sdk` 自动配置。
- 不在 starter 中实现业务接口权限、数据权限、资源树注册；这些仍由下游业务服务的 `iam-sdk` 或业务代码处理。

## 平台能力归属判定

| 能力 | 判定 | 是否可进平台 | 依据 | 兼容或替换策略 |
|---|---|---:|---|---|
| Spring Cloud Gateway 自动装配 | ENHANCE_PLATFORM | 是 | 新项目天然横向需要，且无业务语义 | 通过 starter 自动配置提供 |
| route 级认证策略解析 | ENHANCE_PLATFORM | 是 | `none/jwt/hmac/jwt-or-hmac` 是网关基础策略 | 配置驱动 |
| JWT 解密、验签、session、blacklist、user-enabled | ENHANCE_PLATFORM | 是 | 用户请求标准安全链，可复用 | 配置 claims/key/header 映射 |
| JWT nonce 防重放 | ENHANCE_PLATFORM | 是 | 用户请求重放防护基础能力 | 第一版 `simple-setnx`，预留 `pool` |
| HMAC timestamp、bodyDigest、nonce、signature | ENHANCE_PLATFORM | 是 | 系统对系统调用标准安全链 | 标准 canonical string |
| app credential resolver | ENHANCE_PLATFORM | 是 | HMAC 需要通用凭证解析 | Redis + config fallback，支持自定义 bean |
| 可信 header 清理 | ENHANCE_PLATFORM | 是 | 防止客户端伪造下游上下文 | 全路径始终执行 |
| 上下文 header 注入 | ENHANCE_PLATFORM | 是 | 下游服务统一消费认证上下文 | 只注入 starter 验证后的上下文 |
| TraceId、request log、error writer | ENHANCE_PLATFORM | 是 | 通用可观测与错误响应 | starter 默认启用，可配置 |
| Sentinel Gateway / Knife4j Gateway 集成 | ENHANCE_PLATFORM | 是 | 网关常见横向能力 | 可选开关 |
| 路由表和业务路径 | DOMAIN_ONLY | 否 | 每个系统自己的拓扑 | 留在配置 |
| todo 灰度、IAM admin 特判、process admin confirm | DOMAIN_ONLY | 否 | 业务或产品语义 | 由业务服务或项目扩展点处理 |

平台版本影响：新增平台模块并纳入 `platform-bom` 管理。

消费者影响：新项目 gateway 模块只需引入 starter 并声明配置；下游业务服务继续使用已有平台 web/security/iam 能力消费可信头。

风险：如果 starter 直接吸收历史项目特殊规则，会污染平台边界；第一版只做 `standard` 模式，历史项目仅作为实现参考和测试样本。

## 目标运行形态

新项目 gateway 模块保持极薄：

```text
xxx-gateway
  src/main/java/.../XxxGatewayApplication.java
  src/main/resources/application.yml
  pom.xml
```

依赖：

```xml
<dependency>
  <groupId>com.maritime.platform</groupId>
  <artifactId>platform-gateway-starter</artifactId>
</dependency>
```

启动类：

```java
@SpringBootApplication
public class XxxGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(XxxGatewayApplication.class, args);
    }
}
```

## 模块与包结构

新增模块：

```text
platform-gateway-starter
```

建议包结构：

```text
com.maritime.platform.gateway.autoconfigure
com.maritime.platform.gateway.config
com.maritime.platform.gateway.filter
com.maritime.platform.gateway.security
com.maritime.platform.gateway.security.jwt
com.maritime.platform.gateway.security.hmac
com.maritime.platform.gateway.security.nonce
com.maritime.platform.gateway.header
com.maritime.platform.gateway.error
```

## 认证模式

第一版支持：

```text
none
jwt
hmac
jwt-or-hmac
```

预留：

```text
jwt-and-hmac
```

`jwt-and-hmac` 用于未来高安全代办或代理请求，要求同时验证用户 JWT 和系统 HMAC。第一版可以保留枚举和配置校验，但不默认开放业务使用。

默认认证模式：

```yaml
maritime.gateway.security.default-auth-mode: jwt
```

## JWT 用户请求链路

用户请求标准链：

```text
1. 清理客户端传入的可信 header
2. 提取 Authorization: Bearer <token>
3. 解密 token，前提是 encrypted-jwt=true
4. 验证 JWT 签名、issuer、exp、clock skew
5. 校验必要 claims：userId、sessionId
6. 校验 session：Redis hasKey iam:session:{sessionId}
7. 校验 blacklist：Redis hasKey iam:token:blacklist:{jti}
8. 校验 user-enabled：iam:user:enabled:{userId} != 0
9. 校验 nonce：按配置对写请求或全部请求要求 X-Nonce
10. 注入标准用户上下文 header
11. 转发下游
```

第一版 JWT nonce 默认使用 `simple-setnx`：

```text
key: platform:gateway:jwt:nonce:{sessionId}:{nonce}
ttl: 5m
```

默认只对写方法要求 nonce：

```text
POST, PUT, PATCH, DELETE
```

## HMAC 系统请求链路

系统对系统请求标准链：

```text
1. 清理客户端传入的可信 header
2. 提取 appKey、timestamp、nonce、bodyDigest、signature
3. 校验 timestamp 窗口
4. 校验 nonce 长度
5. Redis SETNX 防重放
6. 解析 appSecret：Redis / 配置 / 自定义 resolver
7. 校验 app enabled
8. 构造 canonical string 并重算 HMAC
9. 注入标准应用上下文 header
10. 转发下游
```

标准请求 header：

```text
X-App-Key
X-Timestamp
X-Nonce
X-Body-Digest
X-Signature
```

标准 canonical string：

```text
appKey={appKey}
method={HTTP_METHOD}
path={rawPath}
query={canonicalQuery}
timestamp={timestamp}
nonce={nonce}
bodyDigest={sha256Hex(body)}
```

签名：

```text
signature = HMAC_SHA256(appSecret, canonicalString)
```

HMAC nonce key：

```text
platform:gateway:hmac:nonce:{appKey}:{nonce}
```

App credential 默认 Redis key：

```text
iam:app:auth:{appKey}
```

字段约定：

```text
appSecret
appCode
appId
tenantId
tenantCode
permissions
isEnabled
```

## 认证模式语义

`none`：

- 只用于公开路径，例如健康检查、文档、登录入口。
- 不承诺防重放，因为没有可信身份或密钥。
- 仍经过 TraceId、可信头清理、请求日志。

`jwt`：

- 只接受 Bearer token。
- 成功后注入用户上下文。

`hmac`：

- 只接受 HMAC header。
- 成功后注入应用上下文。

`jwt-or-hmac`：

- 有 Bearer token 时走 JWT。
- 没有 Bearer token 时走 HMAC。
- 用于同一路径同时支持用户请求和系统请求的场景。

`jwt-and-hmac`：

- 未来能力。
- 用于高安全代理或代办请求，要求同时知道用户和代理应用。

## 可信 Header 清理

所有路径都先清理客户端传入的可信 header，包括 public path：

```text
X-User-Id
X-User-Name
X-Active-Org-Code
X-Active-Org-Name
X-Tenant-Id
X-Tenant-Code
X-Session-Id
X-System-Scope
X-User-Source
X-Verified-App-Code
X-App-Id
X-App-Code
X-App-Permissions
X-User-Permissions
X-Internal-Call
X-Test-Channel
```

`X-Internal-Call` 必须始终清理，避免外部请求伪造内部调用身份。

## 上下文 Header 注入

JWT 通过后注入：

```text
X-User-Id
X-User-Name
X-Active-Org-Code
X-Active-Org-Name
X-Tenant-Id
X-Session-Id
X-System-Scope
X-User-Source
```

HMAC 通过后注入：

```text
X-Verified-App-Code
X-App-Code
X-App-Id
X-Tenant-Code
X-Tenant-Id
X-App-Permissions
```

权限头默认不主动调用 IAM query-service 获取完整权限。第一版只注入 JWT claims 或 app credential 中已经存在的上下文。需要业务权限和数据权限时，下游服务继续使用 `iam-sdk`。

## 配置模型

基础配置：

```yaml
maritime:
  gateway:
    security:
      default-auth-mode: jwt
      public-paths:
        - /actuator/**
        - /v3/api-docs/**
        - /swagger-ui/**
        - /doc.html
        - /webjars/**
      routes:
        - id: app-api
          paths:
            - /api/**
          auth-mode: jwt
        - id: system-api
          paths:
            - /openapi/**
          auth-mode: hmac
```

JWT 配置：

```yaml
maritime:
  gateway:
    security:
      jwt:
        enabled: true
        encrypted: true
        secret: ${JWT_SECRET}
        issuer: maritime-platform
        clock-skew-seconds: 30
        claims:
          user-id: userId
          user-name: userName
          session-id: sessionId
          active-org-code: activeOrgCode
          active-org-name: activeOrgName
          system-scope: systemScope
          user-source: userSource
          tenant-id: tenantId
        redis-keys:
          session-prefix: iam:session:
          blacklist-prefix: iam:token:blacklist:
          user-enabled-prefix: iam:user:enabled:
        validation:
          require-session: true
          check-blacklist: true
          check-user-enabled: true
          user-enabled-disabled-value: "0"
        nonce:
          enabled: true
          mode: simple-setnx
          required-methods: POST,PUT,PATCH,DELETE
          ttl: 5m
          simple-key-prefix: platform:gateway:jwt:nonce:
```

HMAC 配置：

```yaml
maritime:
  gateway:
    security:
      hmac:
        enabled: true
        timestamp-tolerance: 5m
        min-nonce-length: 16
        nonce-key-prefix: platform:gateway:hmac:nonce:
        nonce-ttl: 5m
        headers:
          app-key: X-App-Key
          timestamp: X-Timestamp
          nonce: X-Nonce
          body-digest: X-Body-Digest
          signature: X-Signature
        credentials:
          source: redis-with-config-fallback
          redis-key-prefix: iam:app:auth:
          fields:
            app-secret: appSecret
            app-code: appCode
            app-id: appId
            tenant-id: tenantId
            tenant-code: tenantCode
            permissions: permissions
            enabled: isEnabled
          apps:
            - app-key: demo-key
              app-secret: demo-secret
              app-code: demo
              enabled: true
```

## 过滤器顺序

```text
HIGHEST_PRECEDENCE + 0
TraceIdGatewayFilter

HIGHEST_PRECEDENCE + 5
UntrustedHeaderStripFilter

HIGHEST_PRECEDENCE + 10
RequestLogGatewayFilter

HIGHEST_PRECEDENCE + 20
RouteSecurityPolicyFilter

HIGHEST_PRECEDENCE + 30
ContextHeaderInjectionFilter
```

认证结果通过 exchange attribute 在内部传递，不使用客户端原始 header 作为可信上下文。

## 内部组件

```text
RouteSecurityPolicyResolver
JwtAuthenticationManager
JwtStateValidator
JwtNonceValidator
HmacAuthenticationManager
HmacCanonicalRequestBuilder
AppCredentialResolver
GatewayPrincipal
TrustedHeaderWriter
GatewayErrorWriter
GatewaySecurityProperties
```

可覆盖扩展点：

```text
AppCredentialResolver
JwtClaimsMapper
GatewayPrincipalHeaderCustomizer
GatewaySecurityPolicyCustomizer
GatewayErrorWriter
```

扩展点用于特殊项目，不是新项目接入的必要步骤。

## 错误响应

默认 JSON 响应：

```json
{"code":401,"message":"MISSING_TOKEN","data":null}
```

典型错误码：

```text
MISSING_TOKEN
INVALID_TOKEN
TOKEN_EXPIRED
SESSION_EXPIRED
TOKEN_BLACKLISTED
USER_DISABLED
NONCE_REQUIRED
NONCE_INVALID
MISSING_HMAC_HEADERS
TIMESTAMP_EXPIRED
REPLAY_DETECTED
UNKNOWN_APP
APP_DISABLED
INVALID_SIGNATURE
FORBIDDEN
```

错误响应应包含 `X-Trace-Id`，便于排查。

## 测试策略

单元测试：

- route auth-mode 匹配。
- JWT claims 映射。
- session、blacklist、user-enabled 校验分支。
- simple-setnx nonce 成功、重复、过期行为。
- HMAC canonical string 稳定性。
- HMAC timestamp、nonce、signature 校验。
- 可信 header 清理。
- 用户和应用上下文 header 注入。

集成测试：

- 使用 Spring Cloud Gateway 测试请求流。
- 使用 Testcontainers Redis 验证 nonce、session、blacklist、app credential。
- 验证 `none/jwt/hmac/jwt-or-hmac` 四种模式。
- 验证缺省配置可启动。

## 验收标准

- 新项目 gateway 不写认证过滤器，只引入 starter 和配置即可运行。
- 标准 JWT 请求具备 JWT 验证、session、blacklist、user-enabled、nonce 防护。
- 标准 HMAC 请求具备 timestamp、bodyDigest、nonce、防重放和签名校验。
- 所有路径都会清理可信 header。
- 下游服务只能收到 starter 认证后注入的上下文 header。
- 平台 starter 不包含 todo、process、IAM 管理端等业务规则。
