# platform-gateway-starter

Spring Cloud Gateway 统一认证与安全过滤 starter。新项目引入此 starter 并完成配置，即可获得 JWT 用户认证和 HMAC 系统间签名认证能力，无需重复开发网关安全层。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.maritime.platform</groupId>
    <artifactId>platform-gateway-starter</artifactId>
    <version>1.0.9</version>
</dependency>
```

### 2. 启动类

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

无需 `@EnableXxx` 注解，starter 通过 Spring Boot 自动配置生效。

### 3. 最小 `application.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: your-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/**

maritime:
  gateway:
    security:
      # 默认认证模式（JWT 用户请求）
      default-auth-mode: jwt
      # 公开路径（跳过认证）
      public-paths:
        - /api/public/**
        - /actuator/health
      jwt:
        enabled: true
        secret: <your-jwt-hmac-secret>
        issuer: <your-jwt-issuer>
      hmac:
        enabled: true
```

> 需要 Redis 连接。JWT nonce、session、blacklist、HMAC nonce 和 app credential 均依赖 Redis。

## 认证模式

| 模式 | 说明 |
|------|------|
| `NONE` | 跳过认证，仅保留 header 清理和 trace ID 注入 |
| `JWT` | Bearer token 认证，适用于用户请求 |
| `HMAC` | 签名认证，适用于系统间调用 |
| `JWT_OR_HMAC` | JWT 或 HMAC 二选一，先尝试 JWT，无 Bearer token 则走 HMAC |
| `JWT_AND_HMAC` | **预留能力，当前版本不可用。** 配置后启动即报错 |

认证模式可在三个层级指定（优先级从高到低）：路由规则 → 默认模式。`public-paths` 列表中的路径始终为 `NONE`。

```yaml
maritime:
  gateway:
    security:
      default-auth-mode: jwt-or-hmac
      routes:
        - id: external-api
          paths:
            - /api/external/**
          auth-mode: hmac                # 仅系统调用
        - id: user-api
          paths:
            - /api/user/**
          auth-mode: jwt                 # 仅用户请求
```

## JWT 用户请求认证

### 请求标准

客户端在 `Authorization: Bearer <token>` 中携带 JWT。对 POST/PUT/PATCH/DELETE 请求，还需携带 `X-Nonce` 防重放。

### 服务端校验链

1. 提取 Bearer token → 缺失则返回 `MISSING_TOKEN`
2. 解密（若 `jwt.encrypted=true`）→ 失败则返回 `INVALID_TOKEN`
3. 验证签名、issuer、过期 → 失败则返回 `INVALID_TOKEN` / `TOKEN_EXPIRED`
4. 检查 session 存在于 Redis → 不存在则返回 `SESSION_EXPIRED`
5. 检查 JTI 不在 Redis 黑名单 → 在黑名单则返回 `TOKEN_BLACKLISTED`
6. 检查用户启用状态 → 禁用则返回 `USER_DISABLED`
7. 写请求校验 `X-Nonce` 防重放 → 缺失返回 `NONCE_REQUIRED`，重复返回 `REPLAY_DETECTED`

### JWT 校验开关

```yaml
jwt:
  validation:
    require-session: true        # 是否校验 session
    check-blacklist: true        # 是否校验黑名单
    check-user-enabled: true     # 是否校验用户启用状态
  nonce:
    enabled: true                # 是否启用写请求 nonce 防重放
```

## HMAC 系统请求认证

### 请求标准

调用方在请求中携带以下 header（所有 header 名称均可通过配置覆盖）：

| Header | 说明 | 配置键 |
|--------|------|--------|
| `X-App-Key` | 应用标识 | `maritime.gateway.security.hmac.headers.app-key` |
| `X-Timestamp` | Unix 时间戳（毫秒），允许偏差默认 5 分钟 | `maritime.gateway.security.hmac.headers.timestamp` |
| `X-Nonce` | 随机字符串，最短 16 字符，防重放 | `maritime.gateway.security.hmac.headers.nonce` |
| `X-Body-Digest` | 请求体 SHA-256 hex | `maritime.gateway.security.hmac.headers.body-digest` |
| `X-Signature` | HMAC-SHA256 签名 | `maritime.gateway.security.hmac.headers.signature` |

签名 base string 为换行符连接的规范请求（由 starter 内部构建），密钥为 `appSecret`。

### HMAC 签名算法

调用方按以下规则构造 canonical string 并使用 `appSecret` 签名：

**Canonical string 格式（7 行，换行符 `\n` 分隔）：**

```text
appKey={appKey}
method={HTTP_METHOD}
path={rawPath}
query={canonicalQuery}
timestamp={timestamp}
nonce={nonce}
bodyDigest={sha256Hex(body)}
```

**各字段规则：**

| 字段 | 规则 |
|------|------|
| `appKey` | `X-App-Key` header 值 |
| `method` | HTTP 方法，**大写**（如 `GET`、`POST`） |
| `path` | 原始 URI path（不含 query string），如 `/api/data`。无 path 时为空行值 |
| `query` | **按 key 字典序排序**，值 **URL-encode**（空格编码为 `%20`，非 `+`）。无 query 时为空行值 |
| `timestamp` | `X-Timestamp` header 值，**epoch 毫秒**字符串（如 `1700000000000`） |
| `nonce` | `X-Nonce` header 值，最短 16 字符 |
| `bodyDigest` | 请求体原始字节的 **SHA-256 小写 hex**（无 body 时为空字节数组的 SHA-256） |

**签名计算：**

```text
signature = HMAC-SHA256(appSecret, canonicalString)  输出为小写 hex
```

> **⚠️ 签名契约不兼容：** `platform-common-core` 中的 `HmacSignatureValidator` 使用旧格式 `systemCode=...&timestamp=...&nonce=...&bodyDigest=...`，与 gateway starter v2 **不兼容**。旧 helper 仅供历史系统（如 `iam-sdk` 的 `HmacSignatureGenerator`）内部使用。对 gateway starter 的 HMAC 认证，必须按本文档的 7 行带字段名格式签名。需要同时对接新老格式的系统应显式使用不同的 helper 或 `legacy` 模式，不可静默混用。

**可复现签名示例：**

```text
appKey    = demo-app
appSecret = demo-secret
method    = POST
path      = /api/echo
query     = b=2&a=1          → canonicalQuery = a=1&b=2
timestamp = 1700000000000
nonce     = nonce0123456789ab
body      = (empty)
bodyDigest = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

Canonical string：

```text
appKey=demo-app
method=POST
path=/api/echo
query=a=1&b=2
timestamp=1700000000000
nonce=nonce0123456789ab
bodyDigest=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

调用方用 `demo-secret` 对以上 canonical string 做 HMAC-SHA256 得到签名：

```text
signature = 45d65010145d3d035a883a6ebf2d33f8c0d20cfb95e2060402110aa1255f0da9
```

调用方可用标准工具（如 `openssl dgst -sha256 -hmac demo-secret`）自行验算 canonical string 与签名结果。

> **自定义 header 名称：** 五个 HMAC 入站 header 均可通过配置改为自定义名称，例如 `hmac.headers.app-key=X-App-Code`。自定义名称在认证前会被 `UntrustedHeaderStripFilter` 保留，认证后由 `HmacAuthenticationGatewayFilter` 移除，上下文注入阶段再由 `ContextHeaderInjectionFilter` 写入已验证的值。同名 header 的生命周期是：客户端原始值 → 网关认证读取 → 网关移除 → 网关注入已验证值。

### 服务端校验链

1. 检查 header 完整性 → 缺失返回 `MISSING_HMAC_HEADERS`
2. 校验时间戳在容忍窗口内 → 超时返回 `TIMESTAMP_EXPIRED`
3. 校验 nonce 长度 ≥ 16（可配）→ 过短返回 `INVALID_SIGNATURE`
4. 对比 body digest → 不匹配返回 `INVALID_SIGNATURE`
5. nonce 防重放（Redis SETNX）→ 重复返回 `REPLAY_DETECTED`
6. 查找 app credential（Redis 优先，config fallback）→ 未找到返回 `UNKNOWN_APP`
7. 检查 app 启用状态 → 禁用返回 `APP_DISABLED`
8. 验证 HMAC 签名 → 失败返回 `INVALID_SIGNATURE`

### App Credential 配置

默认从 Redis hash `iam:app:auth:{appKey}` 读取，也可在配置中静态声明 fallback：

```yaml
hmac:
  credentials:
    apps:
      - app-key: my-system
        app-secret: <shared-secret>
        app-code: MY_SYS
```

## Header 清理与注入

### 过滤链

过滤器按固定顺序执行，保证安全语义：

| 顺序 | 过滤器 | 作用 |
|------|--------|------|
| 1 | TraceIdGatewayFilter | 捕获或生成 trace ID |
| 2 | UntrustedHeaderStripFilter | 清除所有不信任的内部 header |
| 3 | RequestLogGatewayFilter | 调试日志 |
| 4 | RouteSecurityPolicyFilter | 解析路由认证策略 |
| 5 | JwtAuthenticationGatewayFilter | JWT 认证 |
| 6 | JwtNonceGatewayFilter | JWT nonce 防重放 |
| 7 | HmacAuthenticationGatewayFilter | HMAC 认证 |
| 8 | ContextHeaderInjectionFilter | 注入已验证的身份 header |

### 清理清单

以下可信上下文 header **对所有请求**（含公开路径）无条件移除，确保客户端无法伪造内部身份：

```
X-Internal-Call
X-User-Id, X-User-Name
X-Active-Org-Code, X-Active-Org-Name
X-Session-Id
X-System-Scope
X-User-Source
X-Tenant-Id, X-Tenant-Code
X-App-Code, X-App-Id
X-Verified-App-Code
X-App-Permissions
X-User-Permissions
X-Test-Channel
X-Trace-Id
```

> **HMAC 签名 header 生命周期：**
> 
> 1. **保留阶段：** `UntrustedHeaderStripFilter` 不会清理当前配置的 HMAC 签名 header（`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature` 及其自定义名称），以便 HMAC 认证过滤器读取签名材料。
> 2. **认证后移除：** `HmacAuthenticationGatewayFilter` 在 HMAC 认证决策后移除这些签名 header（包括成功、失败、或非 HMAC 路径）。
> 3. **最终剥离：** `ContextHeaderInjectionFilter` 在转发下游前**无条件**剥离所有已知 HMAC 签名 header —— 包括默认名称（`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Body-Digest`、`X-Signature`）和当前配置的自定义名称。此步骤在 `hmac.enabled=false`、JWT-only、`auth-mode=none` 等所有模式下均生效，确保原始签名材料在任何场景下都不会到达下游业务服务。
>
> **TraceId 透传：** 客户端传入的 `X-Trace-Id` 在 `TraceIdGatewayFilter`（第一道过滤）中被捕获并规范化，随后被 `UntrustedHeaderStripFilter` 移除原始客户端 header，最后在 `ContextHeaderInjectionFilter` 中写入 gateway 确认后的值。下游服务因此总能收到 sanitized `X-Trace-Id`，且与响应中的 `X-Trace-Id` 一致。

### 注入清单

**JWT 用户认证通过后注入：**

```
X-User-Id, X-User-Name
X-Active-Org-Code, X-Active-Org-Name
X-Tenant-Id
X-Session-Id
X-System-Scope        (List → 逗号拼接)
X-User-Source
```

**HMAC 应用认证通过后注入：**

```
X-Verified-App-Code, X-App-Code   (均设置为 appCode)
X-App-Id
X-Tenant-Code, X-Tenant-Id
X-App-Permissions                  (List → 逗号拼接)
```

## 统一错误响应

所有认证错误返回统一 JSON 格式：

```json
{
  "code": 401,
  "message": "MISSING_TOKEN",
  "data": null
}
```

响应头包含 `X-Trace-Id` 用于链路追踪。

### 错误码速查

| 错误码 | HTTP | 触发条件 |
|--------|------|----------|
| `MISSING_TOKEN` | 401 | 缺少 Bearer token |
| `INVALID_TOKEN` | 401 | JWT 解析/签名/claims 失败 |
| `TOKEN_EXPIRED` | 401 | JWT 过期 |
| `SESSION_EXPIRED` | 401 | Redis 中无 session |
| `TOKEN_BLACKLISTED` | 401 | JTI 在黑名单中 |
| `USER_DISABLED` | 401 | 用户被禁用 |
| `NONCE_REQUIRED` | 401 | 写请求缺少 X-Nonce |
| `REPLAY_DETECTED` | 401 | nonce 重复（JWT 或 HMAC） |
| `MISSING_HMAC_HEADERS` | 401 | HMAC header 缺失 |
| `TIMESTAMP_EXPIRED` | 401 | 时间戳超出容忍窗口 |
| `INVALID_SIGNATURE` | 401 | HMAC 签名/body digest/哈希不匹配 |
| `UNKNOWN_APP` | 401 | appKey 未找到 |
| `APP_DISABLED` | 401 | 应用被禁用 |
| `FORBIDDEN` | 403 | 预留，当前未使用 |
| `UNSUPPORTED_AUTH_MODE` | 501 | 配置了 JWT_AND_HMAC |

## 与 iam-sdk 的关系

`platform-gateway-starter` **不依赖** `iam-sdk`，两者在编译期独立，在运行期通过共享 Redis key 约定协作：

| Redis Key 前缀 | 写入方 | 读取方 (gateway-starter) |
|----------------|--------|--------------------------|
| `iam:session:` | IAM 服务 | JWT session 校验 |
| `iam:token:blacklist:` | IAM 服务 | JWT 黑名单校验 |
| `iam:user:enabled:` | IAM 服务 | 用户启用状态校验 |
| `iam:app:auth:` | IAM 服务 | HMAC app credential 查询 |

所有 Redis key 前缀均可通过配置覆盖，因此耦合是软的。

**下游微服务需要租户、用户、应用权限数据时**，应手动引入 `iam-sdk` 或对应的业务 SDK。gateway-starter 仅在网关层做认证鉴权，不向下游透传 IAM 数据模型。具体来说：

- gateway-starter 通过 header 传递身份标识（userId、tenantId、appCode 等），下游可直接读取这些 header。
- 如需做数据权限过滤、租户隔离等 IAM 领域逻辑，需要 `iam-sdk` 提供的 `@PermissionFilter` 等能力。

## 扩展点

| 接口 | 用途 | 默认实现 |
|------|------|----------|
| `GatewayErrorWriter` | 自定义错误响应格式 | `DefaultGatewayErrorWriter`（JSON 统一格式） |
| `JwtClaimsMapper` | 自定义 JWT claims → Principal 映射 | `DefaultJwtClaimsMapper` |
| `AppCredentialResolver` | 自定义 app credential 获取逻辑 | `DefaultAppCredentialResolver`（Redis + config fallback） |
| `GatewaySecurityPolicyCustomizer` | 编程式注册路由策略 | 无（按需实现 Bean） |
| `GatewayPrincipalHeaderCustomizer` | 注入额外自定义 header | 无（按需实现 Bean） |

实现对应接口并注册为 Spring Bean 即可替换或增强默认行为。

## Starter 范围说明

本 starter 负责**网关层通用认证与安全**，以下内容**不在** starter 范围内：

- Todo / IAM / Process 等特定业务模块的特殊规则
- 业务级别的权限校验（角色、菜单、按钮等）
- 多租户数据隔离逻辑
- 下游微服务框架集成

这些规则由各自的业务模块自行实现。starter 保持通用、无业务耦合。
