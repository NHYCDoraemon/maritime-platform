# Spring Boot 自动配置与 Bean 契约治理

## 背景

`maritime-platform` 同时提供通用基础库、Spring Boot 自动配置和 IAM SDK。
当前多数模块已经通过
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
接入 Spring Boot，但仍存在以下契约不一致：

- 部分默认 Bean 无法被消费方安全替换；
- `@AutoConfiguration` 内直接调用另一个 `@Bean` 方法，绕过容器 Bean；
- Web 与 Gateway starter 通过 `@ComponentScan` 发现内部组件；
- MyBatis-Plus 只提供整体拦截器 Bean，没有组合扩展点；
- 通用 MQ 模块默认装配 IAM 专属拓扑；
- 少数类型同时使用 `@Component` 和显式 `@Bean` 注册。

## 平台能力归属判定

| 能力 | 判定 | 是否可进平台 | 依据 | 落地动作 |
|---|---|---|---|---|
| 默认 Bean 回退与条件装配 | ENHANCE_PLATFORM | 是 | 所有 starter 的稳定横向契约 | 默认策略使用 `@ConditionalOnMissingBean` |
| MyBatis-Plus 拦截器组合 | ENHANCE_PLATFORM | 是 | 已有多个消费方需要追加租户、审计和慢查询能力 | 新增有序 customizer SPI |
| Web/Gateway 显式装配 | ENHANCE_PLATFORM | 是 | starter 不应依赖组件扫描副作用 | 使用显式 `@Bean`，保留原 Bean 名 |
| IAM RabbitMQ 拓扑 | DOMAIN_ONLY | 否（不属于 common） | exchange、queue、routing key 均为 IAM 领域契约 | 先增加兼容开关和废弃标记，下一主版本迁出 |
| 所有安全过滤器任意覆盖 | REJECT_PLATFORMIZATION | 否 | 会破坏固定过滤顺序和 fail-closed 语义 | 只开放已定义的策略 SPI |

## Bean 契约

### 可覆盖默认策略

以下 Bean 是消费方扩展点，平台仅提供默认实现：

- `SnowflakeIdGenerator`
- `JwtTokenProvider`
- IAM SDK 的专用 `RestTemplate`、客户端和映射器
- `GatewayErrorWriter`
- `AppCredentialResolver`
- `JwtClaimsMapper`
- `NotificationDispatcher`
- `LockPort`、`IdempotencyPort` 和 Redis resilience SPI
- `OutboxStore`、`OutboxPoller`
- MyBatis-Plus `MetaObjectHandler`、`ISqlInjector`、`IdentifierGenerator`

默认实现必须使用类型或稳定 Bean 名做 `@ConditionalOnMissingBean` 回退。

### 固定内部组件

Gateway 的可信 Header 清理、认证过滤器、nonce 校验和过滤器顺序属于安全链内部实现。
这些 Bean 由自动配置显式创建并由属性控制启用，不因为出现同类型 Bean 而静默退出。
消费方只能通过已公开的 writer、resolver、mapper 和 customizer SPI 扩展。

### 可组合组件

`MybatisPlusInterceptor` 仍由平台提供默认分页与乐观锁能力，但在返回前按 Spring
顺序执行所有 `MybatisPlusInterceptorCustomizer`。消费方可以追加稳定的
`InnerInterceptor`，也可以提供完整 `MybatisPlusInterceptor` Bean 覆盖平台默认值。

## 兼容策略

1. 保留现有 Bean 名，避免按名注入的消费方失效。
2. 新增回退条件属于 additive change；消费方已有 Bean 将优先于平台默认值。
3. `iam.snowflake.*` 在本次保持兼容；新的配置命名迁移另立版本决策。
4. `IamTopologyConfiguration` 本次保留默认启用，但增加
   `maritime.mq.iam-topology.enabled` 开关和废弃说明。
5. 下一主版本把 IAM 拓扑迁入 IAM 命名模块，并从 `platform-common-mq`
   的自动配置入口移除。
6. 通过 `platform-bom` 发布后，依次验证 `iam-center`、`process-engine`
   和 `todo-center`。

## 验证

- 每个受影响自动配置至少覆盖：默认装配、用户 Bean 回退、前置条件缺失。
- Gateway 覆盖：固定过滤链仍完整、公开扩展点仍可替换、JWT/HMAC 配置继续
  fail closed。
- MyBatis 覆盖：默认拦截器、customizer 顺序和整体 Bean 覆盖。
- MQ 覆盖：兼容默认值、显式关闭和同名拓扑 Bean 回退。
- L3 运行受影响模块测试；Redis/Gateway Testcontainers 需要可用 Docker。
