# Redis 自动配置顺序与 1.0.11 发布设计

## 背景

`platform-common-redis` 的 `LockPortAutoConfiguration`、
`DistributedLockAutoConfiguration`、`IdempotencyAutoConfiguration` 和
`ResilienceAutoConfiguration` 都通过 `@ConditionalOnBean` 等待
`StringRedisTemplate`。但是这些自动配置没有声明在 Spring Boot
`RedisAutoConfiguration` 之后运行，因此条件可能在
`StringRedisTemplate` 注册前被求值并永久回退。

`LeaderElectedAutoConfiguration` 同样依赖平台提供的 `LockPort`，但没有声明
在 `LockPortAutoConfiguration` 之后运行。`process-engine` 当前通过
`PlatformLockPortCompatibilityAutoConfiguration` 和
`PlatformLeaderElectionCompatibilityAutoConfiguration` 重建平台 Bean，
证明该问题已经影响真实消费方。

上一批 Bean 治理代码已经进入 `main`，但根 POM 和所有子模块仍使用
`1.0.10`。该版本已经被消费方使用，不能用相同版本号覆盖发布。

## 目标

1. 平台 Redis 自动配置在标准 Spring Boot Redis/Jackson 自动配置完成后稳定生效。
2. 依赖不存在时继续安全回退，不把 Redis、Jackson 或 AOP 变成强制依赖。
3. 用户提供同类型 Bean 时继续由 `@ConditionalOnMissingBean` 保证覆盖优先。
4. 将平台 reactor 和 BOM 统一准备为不可变的新版本 `1.0.11`。
5. 不在本批修改消费方仓库；消费方在升级 BOM 后单独删除兼容层。

## 平台能力归属判定

| 能力 | 判定 | 是否可进平台 | 依据 | 落地动作 |
|---|---|---|---|---|
| Redis 自动配置排序 | ENHANCE_PLATFORM | 是 | 所有 Redis primitive 共用的 Spring Boot starter 契约 | 在现有自动配置上增加显式 `after` |
| Leader 自动配置排序 | ENHANCE_PLATFORM | 是 | `LeaderElectedAspect` 直接依赖平台 `LockPort` | 排在 `LockPortAutoConfiguration` 之后 |
| process-engine 兼容配置 | REUSE_PLATFORM | 不新增 | 仅用于绕过平台 1.0.10 缺陷 | 消费方升级 1.0.11 并验证后删除 |
| Outbox V2 | ENHANCE_PLATFORM | 是，但不在本批 | 涉及状态机、表结构和并发 claim 契约 | 另立 ADR 和独立发布批次 |

平台版本影响：`platform-bom` 和全部平台模块升级到 `1.0.11`。

消费者影响：本批保持二进制和源码兼容；IAM、Todo、Process 可按各自节奏升级。

## 方案比较

### 方案 A：显式自动配置顺序（采用）

- Redis-backed 配置声明在 Boot `RedisAutoConfiguration` 之后。
- `IdempotencyAutoConfiguration` 同时声明在
  `JacksonAutoConfiguration` 之后。
- `LeaderElectedAutoConfiguration` 声明在
  `LockPortAutoConfiguration` 之后。
- 保留现有 `@ConditionalOnClass`、`@ConditionalOnBean` 和
  `@ConditionalOnMissingBean`。

该方案只修正条件求值时机，不改变 Bean 类型、名称、属性或 SPI。

### 方案 B：移除 `@ConditionalOnBean`（拒绝）

Bean 方法参数会在依赖缺失时造成上下文启动失败，使可选平台模块变成强耦合，
违反 starter 的回退契约。

### 方案 C：消费方继续提供兼容配置（拒绝）

这会让每个消费方重复平台装配细节，并可能形成不同的 key prefix、Bean 名和
启用条件，无法从根因上修复平台契约。

## 代码设计

修改以下自动配置：

- `DistributedLockAutoConfiguration`：排在 Boot Redis 自动配置之后。
- `LockPortAutoConfiguration`：排在 Boot Redis 自动配置之后。
- `ResilienceAutoConfiguration`：排在 Boot Redis 自动配置之后。
- `IdempotencyAutoConfiguration`：排在 Boot Redis 和 Jackson 自动配置之后。
- `LeaderElectedAutoConfiguration`：排在 `LockPortAutoConfiguration` 之后。

不修改现有实现类，不新增公共接口，不改变 `AutoConfiguration.imports` 中的入口。
排序声明使用类型引用；相关 Boot 自动配置类由现有 Spring Boot starter 依赖提供。

## 测试设计

新增一个聚焦的 `ApplicationContextRunner` 测试，组合平台 Redis 自动配置和
Spring Boot 的 Redis/Jackson 自动配置，并仅提供
`RedisConnectionFactory`。测试必须证明：

1. 未修复代码因为自动配置顺序缺失，无法创建依赖 `StringRedisTemplate` 的
   lock、leader、idempotency 和 resilience 默认 Bean。
2. 修复后所有默认 Bean 均被创建。
3. 不提供 `RedisConnectionFactory` 时，这些默认 Bean 均不创建且上下文正常。
4. 用户提供 `LockPort` 或 `IdempotencyPort` 时，平台默认实现回退。

先运行聚焦测试完成 RED/GREEN，再运行 `platform-common-redis` 模块测试，最后在
Finch 环境运行全仓 `mvn test` 和 `mvn -DskipTests package`。

Finch 使用：

- `DOCKER_HOST=unix:///Applications/Finch/lima/data/finch/sock/finch.sock`
- `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
- `TESTCONTAINERS_CHECKS_DISABLE=true`
- Maven JVM 参数 `-Dapi.version=1.43`
- 在测试前预拉取 `testcontainers/ryuk:0.12.0`

## 版本与兼容策略

1. 根 reactor、`platform-bom`、BOM 的 `platform.version` 和所有子模块 parent
   版本统一改为 `1.0.11`。
2. README 的 BOM 使用示例同步为 `1.0.11`。
3. 不覆盖或重新发布 `1.0.10`。
4. 不在本批直接修改 IAM、Todo 或 Process 的依赖版本。
5. `process-engine` 升级并完成 Redis/leader 集成验证后，单独删除两个兼容自动配置。

## 非目标

- 不实现 Outbox V2。
- 不迁移 IAM RabbitMQ 拓扑。
- 不迁移 Todo common 模块。
- 不修改 Redis primitive 的算法、key 格式、超时或序列化行为。
- 不创建 Git tag、不发布 GitHub Packages；发布动作在代码评审和完整验证后单独执行。

## 风险与回滚

- 风险：显式排序使之前因错误顺序而缺失的 Bean 开始正常创建。现有
  `@ConditionalOnMissingBean` 会保护消费方自定义实现。
- 风险：版本遗漏会造成 reactor 内部依赖解析不一致。通过搜索旧版本号和全仓打包
  验证。
- 回滚：排序注解和版本准备均可作为独立提交回退；不会涉及数据或 API 迁移。
