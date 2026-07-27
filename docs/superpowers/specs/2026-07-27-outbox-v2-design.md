# Outbox V2 多副本投递与渐进迁移设计

## 背景

`platform-common-outbox` 当前提供一套最小 Outbox 实现（下文称 V1）：

- `OutboxEventPublisher` 的公共 SPI 直接暴露 MyBatis `OutboxEntryDO`。
- `OutboxStore.findDue()` 先查询、再由轮询器逐条发送，没有原子 claim，
  多实例会并发取得同一行。
- 达到最大重试次数的记录被写成 `FAILED + nextRetryAt = null`，但
  `findDue()` 又会选择 `nextRetryAt IS NULL` 的 `FAILED` 记录，导致日志宣称
  “永久失败”的消息仍会被无限重试。
- 没有 producer 隔离、显式 tenant、claim token、过期 claim 恢复、
  broker confirm/return 结果契约、保留期清理和可靠重放。
- 轮询器直接读取系统时间，测试和运行时策略不易控制。

三个真实消费方已经证明这些能力不是假设需求：

- `iam-center/iam-event-outbox` 已孵化
  `FOR UPDATE SKIP LOCKED`、producer 隔离、`PENDING/PUBLISHING/PUBLISHED/DEAD`
  状态机、过期锁恢复、RabbitMQ confirm/return 校验和保留期清理。
  IAM ADR-016 与实现状态均把“通用化后上收平台”列为后续方向。
- `process-engine` 当前使用平台 V1，但在应用内补充 tenant 绑定、保留期清理和
  重放能力；其文档也记录了 V1 的并发和终态缺口。
- `todo-center` 使用 tenant-schema 和领域表耦合的本地 Outbox，不适合在本批被
  强制迁移。

因此，本批在平台内新增 V2，并保留 V1 兼容路径。IAM、Process、Todo 均不在本批
修改。

## 目标

1. 提供不暴露持久化 DO、与消息中间件无关的稳定公共 API/SPI。
2. 在 Kingbase 多实例环境中原子 claim，避免正常并发轮询造成重复投递。
3. 提供可恢复的 `PENDING → PUBLISHING → PUBLISHED/DEAD` 状态机。
4. 只有 broker 已确认且消息未被退回时才认定发布成功。
5. 支持 producer 隔离、可选 tenant、指数退避、死信重放和已发布记录清理。
6. 保证事件写入可参与调用方现有数据库事务。
7. 以新增 V2 包、配置和表的方式保持 V1 源码、二进制和数据兼容。
8. 修复 V1 终态 `FAILED` 被重新选择的确定性缺陷。
9. 将平台 reactor 与 BOM 准备为新版本 `1.0.12`。

## 非目标

- 不在本批升级或修改 IAM、Process、Todo。
- 不迁移 IAM RabbitMQ exchange、queue、binding 或 retry/DLQ 拓扑。
- 不把 Todo 的 tenant-schema 领域 Outbox 抽入平台。
- 不承诺 exactly-once；V2 保证 at-least-once，消费方仍必须按 `eventId` 幂等。
- 不提供 HTTP 管理接口、管理页面或跨 producer 的批量运维接口。
- 不在平台库中自动执行 DDL。
- 不引入事件类反射或基于 `Class.forName` 的反序列化。
- 不创建 Git tag、不发布 Maven/GitHub Packages。
- 不在 `1.x` 删除 V1。

## 平台能力归属

| 能力 | 判定 | 归属 | 本批动作 |
|---|---|---|---|
| 通用 Outbox 消息模型与 publisher SPI | ENHANCE_PLATFORM | 平台 | 新增 V2 |
| 多副本 claim、过期 claim 恢复和 CAS 终结 | ENHANCE_PLATFORM | 平台 | 新增默认 JDBC 存储 |
| producer/tenant 元数据、重试、重放、保留期 | ENHANCE_PLATFORM | 平台 | 新增通用配置和运维 API |
| RabbitMQ confirm/return 的具体调用 | KEEP_APP_LOCAL | 消费方 adapter | SPI 只定义成功契约 |
| IAM exchange/queue/binding/retry/DLQ | KEEP_APP_LOCAL | IAM | 后续独立批次 |
| Process tenant/replay/cleanup 兼容层 | INCUBATE_APP | Process | 本批保留，迁移后再删 |
| Todo tenant-schema 领域 Outbox | KEEP_APP_LOCAL | Todo | 不迁移 |

平台变更只依赖已经被多个消费方验证的共性。消息拓扑、领域表结构和消费语义继续由
应用拥有。

## 方案比较

### 方案 A：V1/V2 并行、V2 显式启用（采用）

- 在 `com.maritime.platform.common.outbox.v2` 下新增 API、SPI 和实现。
- 新增 `platform_outbox_message`，不复用 V1 表。
- V2 默认关闭；消费方显式启用并提供 V2 publisher。
- V1 API、自动配置和表继续存在，仅做终态查询缺陷修复并标记迁移方向。

该方案让平台先稳定契约，各消费方可独立迁移和回滚；不会因为发布平台版本而改变
现有 Outbox 运行路径。

### 方案 B：原地替换 V1（拒绝）

直接修改 `OutboxEntryDO`、V1 SPI、状态值和表结构会同时破坏源码、二进制和数据
兼容。现有消费方无法用一次平台发布安全完成同步迁移。

### 方案 C：只定义 SPI、不提供默认存储（拒绝）

该方案仍会让 IAM、Process 和后续服务重复实现 claim、CAS、重试与清理，无法消除
已经出现的基础设施分叉。

## 公共 API 与包边界

V2 公共类型放在以下包中：

- `com.maritime.platform.common.outbox.v2.api`
- `com.maritime.platform.common.outbox.v2.spi`

JDBC 行模型、SQL、调度器和状态转换实现放在 `v2.internal`，不作为公共契约。

### 写入命令

`OutboxAppendCommand` 是不可变 record，包含：

| 字段 | 约束 |
|---|---|
| `eventId` | 必填、在 producer 内稳定；用于唯一约束和消费幂等 |
| `tenantId` | 可空；平台不隐式读取任何 TenantContext |
| `aggregateType` | 可空；用于检索和诊断 |
| `aggregateId` | 可空；用于检索和诊断 |
| `eventType` | 必填 |
| `destination` | 必填；含义由 publisher adapter 解释 |
| `routingKey` | 可空 |
| `payload` | 必填原始字符串，通常为 JSON |
| `contentType` | 必填，默认由调用方传 `application/json` |
| `headers` | 不可空的只读 `Map<String, String>`，构造时防御性复制 |

命令不接受事件 `Class<?>`。平台存储并转交原始 payload、事件类型和路由元数据，
避免类加载器耦合与不安全反射。

`OutboxWriter.append(OutboxAppendCommand)` 返回平台消息 ID。默认 JDBC 存储使用
当前应用的同一个 `DataSource` 和 Spring 事务管理器；在调用方
`@Transactional` 方法内执行时，insert 必须参与同一事务。自定义存储实现必须
自行满足同一契约。producer 不由每条命令自由传入，而由 V2 配置固定，避免同一
实例意外跨 producer 写入。

`OutboxWriter` 默认要求
`TransactionSynchronizationManager.isActualTransactionActive()` 为 true；
没有活动 Spring 事务时在 insert 前失败，避免产生“已写 Outbox、业务事务却不
存在”的假原子性。多数据源应用必须提供绑定到正确 DataSource/transaction
manager 的自定义 `OutboxStorage`，不能依赖 primary Bean 的偶然选择。

### 发布视图

`OutboxMessage` 是 publisher 可见的不可变 record，包含：

- 平台消息 ID、`eventId`、producer、可选 tenant；
- aggregate、event type、destination、routing key；
- payload、content type、只读 headers；
- 当前 `attemptCount` 和 `createdAt`。

它不包含 JDBC 实体、Mapper、可变 setter 或数据库版本字段。

`OutboxClaim` 是存储 SPI 返回给 relay 的不可变值，包含
`claimToken + OutboxMessage`。claim token 只用于完成当前 claim 的 CAS，不应作为
业务消息头发送。

`OutboxStatus` 是公共只读状态枚举：`PENDING`、`PUBLISHING`、`PUBLISHED`、
`DEAD`。`OutboxRecord` 用于运维查询，组合消息视图与 status、attempt count、
next attempt、claimed/published time 和截断后的 last error；它同样不是可持久化
DO。

### Publisher SPI

```java
public interface OutboxMessagePublisher {
    OutboxPublishResult publish(OutboxMessage message) throws Exception;
}
```

`OutboxPublishResult` 包含 `confirmed`、`returned` 和可选 `detail`。relay 只在
`confirmed == true && returned == false` 时调用 `markPublished`。以下情况均走
失败状态转换：

- publisher 抛出异常；
- broker 未确认或明确 nack；
- confirm 超时；
- broker 返回不可路由消息；
- publisher 返回 `null` 或不满足成功条件的结果。

具体 RabbitMQ/Kafka/其他中间件 API 留在消费方 adapter；平台不依赖 broker SDK。

### 存储 SPI

`OutboxStorage` 提供以下语义方法：

- `append(producer, command, now)`；
- `claim(request)`，返回带 token 的 claim；
- `markPublished(messageId, claimToken, publishedAt, now)`；
- `markFailed(failureCommand)`；
- `requeueDead(messageId, producer, now)`；
- `findById(messageId, producer)`；
- `listDead(producer, limit)`；
- `deletePublishedBefore(producer, cutoff, limit)`。

`OutboxClaimRequest` 明确携带 producer、batch size、max attempts、当前时间、
过期 claim 截止时间和新 claim token。完成/失败操作必须以
`messageId + status=PUBLISHING + claimToken` 做 CAS，并返回是否成功。返回 false
表示 claim 已失效，relay 只记录告警，不得覆盖新持有者的状态。

消费者可通过提供自己的 `OutboxStorage` Bean 替换默认 JDBC 实现；公共 SPI 不
暴露默认表对应的行对象。

### 运维 API

`OutboxOperations` 是 producer-scoped 的应用服务，只提供：

- `Optional<OutboxRecord> findById(String messageId)`；
- `List<OutboxRecord> listDead(int limit)`，按 `createdAt, id` 从旧到新返回；
- `boolean requeueDead(String messageId)`；
- `int deletePublishedBefore(Instant cutoff, int limit)`。

重放只允许 `DEAD → PENDING`，重置 `attemptCount=0`、立即可投递、清除 claim，
保留 `lastError` 供诊断。清理只删除当前 producer 的
`PUBLISHED` 记录，并受正数 limit 限制。

平台不自动暴露 Controller。应用若需要管理端点，应在自身鉴权和审计边界内调用
该 API。

## 状态机

```text
append
  |
  v
PENDING --claim--> PUBLISHING --confirmed && !returned--> PUBLISHED
  ^                    |
  |                    +--failure, attempts remain--> PENDING(nextAttemptAt)
  |                    |
  |                    +--failure, attempts exhausted--> DEAD
  |                    |
  +----stale claim-----+

DEAD --manual requeue--> PENDING
```

状态语义：

- `PENDING`：未 claim，`nextAttemptAt <= now` 时可选。
- `PUBLISHING`：由 claim token 临时拥有；`claimedAt < staleBefore` 后可被重新
  claim。
- `PUBLISHED`：broker 已确认且没有 return；只允许清理，不允许自动重发。
- `DEAD`：尝试次数耗尽；永不被正常 claim，只能人工重放。

`attemptCount` 在 claim 事务中递增。初始值为 0，第一次 claim 后为 1。配置使用
`maxAttempts`，避免“首次 + 重试次数”的歧义。当失败时
`attemptCount >= maxAttempts` 进入 `DEAD`；否则按封顶指数退避回到
`PENDING`。

发布成功后、`markPublished` 前进程崩溃仍可能造成重发，这是 at-least-once 的
固有窗口。因此 `(producer, eventId)` 唯一约束防止生产端重复 insert，消费端仍
必须按 producer 与 `eventId` 的事件来源组合幂等。

## 默认 JDBC 存储

### 表结构

V2 使用独立表 `platform_outbox_message`：

| 列 | 类型/约束 |
|---|---|
| `id` | `VARCHAR(64)` 主键；平台生成 UUID，避免依赖数据库 identity 语法 |
| `event_id` | `VARCHAR(128) NOT NULL` |
| `producer` | `VARCHAR(128) NOT NULL` |
| `tenant_id` | `VARCHAR(128) NULL` |
| `aggregate_type` | `VARCHAR(128) NULL` |
| `aggregate_id` | `VARCHAR(128) NULL` |
| `event_type` | `VARCHAR(256) NOT NULL` |
| `destination` | `VARCHAR(256) NOT NULL` |
| `routing_key` | `VARCHAR(256) NULL` |
| `payload` | `TEXT NOT NULL` |
| `content_type` | `VARCHAR(128) NOT NULL` |
| `headers_json` | `TEXT NOT NULL` |
| `status` | `VARCHAR(16) NOT NULL` |
| `attempt_count` | `INTEGER NOT NULL` |
| `next_attempt_at` | `TIMESTAMP WITH TIME ZONE NULL`；仅 `DEAD`/`PUBLISHED` 可空 |
| `claim_token` | `VARCHAR(64) NULL` |
| `claimed_at` | `TIMESTAMP WITH TIME ZONE NULL` |
| `published_at` | `TIMESTAMP WITH TIME ZONE NULL` |
| `last_error` | `VARCHAR(1000) NULL` |
| `created_at` | `TIMESTAMP WITH TIME ZONE NOT NULL` |
| `updated_at` | `TIMESTAMP WITH TIME ZONE NOT NULL` |

索引：

- 唯一索引 `(producer, event_id)`；
- claim 索引 `(producer, status, next_attempt_at, created_at, id)`；
- 过期 claim 索引 `(producer, status, claimed_at)`；
- 清理索引 `(producer, status, published_at)`。

时间统一以 UTC `Instant` 写入。`headers` 使用现有 Jackson 依赖序列化为 JSON；
读取后返回不可变 Map。

平台提供 Kingbase 兼容的参考 DDL 资源和 README，但不自动建表。消费方必须把
DDL 纳入自己的 Flyway/Liquibase 迁移并拥有上线、回滚和权限控制。

### 原子 claim 算法

每次 relay tick 生成一个随机 claim token，在一个短事务中：

1. 先把已过期且 `attempt_count >= maxAttempts` 的 `PUBLISHING` 转为 `DEAD`，
   防止实例反复崩溃时无限重新 claim。
2. 按当前 producer 选择 `attempt_count < maxAttempts` 的到期 `PENDING`，以及
   `claimedAt < staleBefore` 的 `PUBLISHING`。
3. 按 `created_at, id` 排序并限制 batch size。
4. 使用 `FOR UPDATE SKIP LOCKED` 锁定候选行。
5. 将所选行更新为 `PUBLISHING`，写入新 token/claimedAt，
   `attempt_count + 1`。
6. 在同一事务内读取并返回 claim 快照。

事务提交后才调用 publisher，避免持有数据库锁等待 broker。多个平台实例会跳过
其他实例已锁定的行；过期 `PUBLISHING` 可在实例崩溃后被恢复。

完成和失败更新均做 claim-token CAS：

- 成功：转为 `PUBLISHED`，写入 `publishedAt`，清除 next attempt、claim 和错误。
- 可重试失败：转为 `PENDING`，写入下一次时间和截断后的错误，清除 claim。
- 终态失败：转为 `DEAD`，清除下一次时间和 claim，保留截断后的错误。

`DEAD` 不出现在 claim SQL 中。

## 重试与配置

配置前缀为 `platform.outbox.v2`：

| 属性 | 默认值 | 说明 |
|---|---:|---|
| `enabled` | `false` | V2 总开关 |
| `producer` | `${spring.application.name}` | producer 隔离；两者均为空则启动失败 |
| `relay-enabled` | `true` | writer-only 节点可关闭 relay |
| `poll-interval` | `1s` | relay 间隔 |
| `batch-size` | `100` | 单次 claim 上限 |
| `claim-timeout` | `2m` | 过期 claim 恢复阈值 |
| `max-attempts` | `5` | 包含首次发布 |
| `initial-backoff` | `1s` | 首次失败后的等待 |
| `backoff-multiplier` | `2.0` | 指数退避倍数 |
| `max-backoff` | `5m` | 退避上限 |
| `cleanup-enabled` | `false` | 自动清理默认关闭，避免隐式删数据 |
| `cleanup-interval` | `1h` | 启用自动清理后的执行间隔 |
| `retention` | `7d` | 只影响 `PUBLISHED` |
| `cleanup-batch-size` | `500` | 单次有界删除 |

所有数量和 Duration 在绑定后校验：batch/max attempts 必须大于 0，Duration 必须
为正，multiplier 不小于 1。非法配置在启动时失败，不在调度线程中静默回退。

退避公式为：

```text
min(initialBackoff * multiplier^(attemptCount - 1), maxBackoff)
```

调度、写入和状态转换只使用注入的 `Clock`。自动配置通过
`ObjectProvider<Clock>` 使用消费方 Bean；不存在时内部回退到
`Clock.systemUTC()`，不向应用上下文注册全局 Clock Bean。

## Spring Boot 自动配置

V2 自动配置与 V1 分离，并登记到 `AutoConfiguration.imports`：

1. `OutboxV2AutoConfiguration`
   - 仅在 `platform.outbox.v2.enabled=true` 时启用；
   - 默认 JDBC `OutboxStorage` 仅在存在 `JdbcTemplate` 和
     `PlatformTransactionManager` 且没有自定义 `OutboxStorage` 时创建；
   - 基于最终选定的 `OutboxStorage` 创建 `OutboxWriter` 和
     `OutboxOperations`；如果既没有 JDBC 条件也没有自定义存储，启动失败；
   - 每个默认 Bean 都使用 `@ConditionalOnMissingBean`。
2. `OutboxV2RelayAutoConfiguration`
   - 排在核心 V2 配置之后；
   - 仅在 `relay-enabled=true` 时创建 relay 和调度器；
   - 方法参数要求唯一的 `OutboxMessagePublisher`。开启 relay 却未提供
     publisher 时必须启动失败，不能静默禁用。

开启 V2 是显式动作，因此 V2 调度配置可以启用 Spring scheduling。writer-only
部署将 `relay-enabled=false`，无需 publisher，也不启动调度。

V1 和 V2 使用不同 SPI、自动配置与表。仅提供 V2 publisher 不会激活 V1；同时
提供两种 publisher 时两套 relay 可并存，但迁移方不得把同一业务事件双写到两套
表。

## V1 兼容修复

V1 不改变表、枚举或公共方法签名。只将 `findDue()` 选择条件修正为：

```text
(status = PENDING AND (next_retry_at IS NULL OR next_retry_at <= now))
OR
(status = FAILED AND next_retry_at IS NOT NULL AND next_retry_at <= now)
```

这样：

- 初始 `PENDING + null` 仍可立即发送；
- 等待重试的 `FAILED + future/due time` 保持原行为；
- 达到上限的 `FAILED + null` 成为真实终态，不再被选择。

V2 稳定入口完成后，对 V1 的 `OutboxEntryDO`、`OutboxStore`、
`OutboxPoller` 和 `OutboxEventPublisher` 添加
`@Deprecated(since = "1.0.12", forRemoval = false)` 与迁移 Javadoc。该标记不
改变 V1 运行时行为、Bean 名或二进制链接。

## 错误处理与可观测性

- append 校验失败直接抛出参数异常，不写不完整消息。
- 同一 producer 内的 `eventId` 重复由唯一约束拒绝；不把不同 payload 的重复
  event 静默视为成功。
- publisher 失败必须持久化状态和截断后的错误，并记录 message ID、event ID、
  producer、attempt count；日志不输出 payload。
- CAS 失败记录告警，不覆盖其他实例的新 claim。
- relay 单条失败不阻断同一批其余消息。
- 数据库 claim 事务失败时整批回滚，本轮不发布未成功 claim 的消息。
- 本批不强制依赖 metrics/tracing 模块；后续可在稳定 API 上增加观察器，不改变
  存储和 publisher 契约。

## 测试与验证

### TDD 聚焦测试

先写失败测试，再实现：

1. V1 终态 `FAILED + null` 不再出现在 due 列表。
2. append 校验、header 防御性复制、无活动事务时拒绝写入，以及业务事务回滚时
   Outbox insert 一并回滚。
3. 两个独立 JDBC storage/relay 实例并发 claim 时结果不相交。
4. 过期 `PUBLISHING` 可恢复，未过期 claim 不可抢占。
5. 旧 token 无法 mark published/failed。
6. attempt 计数、指数退避和达到上限转 `DEAD`。
7. `DEAD` 不自动 claim，人工 requeue 后恢复为 `PENDING`。
8. 只有 `confirmed && !returned` 转 `PUBLISHED`。
9. publisher 异常、nack、timeout、return 和 null result 均进入失败路径。
10. producer 隔离、可选 tenant、event ID 唯一约束和有界清理。
11. V2 默认关闭、显式启用、用户 Bean 覆盖、writer-only 模式和缺 publisher
    启动失败。
12. V1 自动配置和现有测试保持通过。

### L1-L5

- L1：编译、格式/静态检查、公共 API 不依赖 `dataobject`/Mapper。
- L2：`platform-common-outbox` 单元和自动配置测试。
- L3：通过 Testcontainers 连接 Finch 中的 Kingbase，验证真实 DDL、claim SQL、
  并发事务和时区映射。
- L4：全仓 `mvn test` 与 `mvn -DskipTests package`。
- L5：平台仓库不拥有生产环境，本批以消费方后续集成验证代替；不宣称完成消费方
  迁移。

Finch 验证环境：

- `DOCKER_HOST=unix:///Applications/Finch/lima/data/finch/sock/finch.sock`
- `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
- `TESTCONTAINERS_CHECKS_DISABLE=true`
- Maven JVM 参数 `-Dapi.version=1.43`

设计基线已经在该环境执行全仓测试：16 个 reactor 模块，568 tests，0 failures，
0 errors，0 skipped。

## 版本与兼容策略

1. 根 reactor、全部子模块 parent、`platform-bom` 和 BOM 中
   `platform.version` 统一升级到 `1.0.12`。
2. README 的 BOM 示例同步为 `1.0.12`，Outbox 文档同时说明 V1/V2、配置、DDL、
   publisher 成功契约和迁移顺序。
3. V2 默认关闭，不会因依赖升级自动建表、启动 relay 或改变 V1。
4. V1 表与公共签名保留；确定性终态查询缺陷按上述规则修复。
5. 本批不修改任何消费方版本；IAM/Process/Todo 可在独立任务中迁移。
6. V1 只允许在未来主版本中删除，删除前必须完成消费者盘点和迁移公告。

## 后续迁移顺序

本批完成后，推荐按以下独立任务推进，但它们不属于本设计的实现范围：

1. IAM 用 V2 adapter 替换孵化实现，并验证 RabbitMQ confirm/return、重放和清理。
2. Process 在 V2 集成测试通过后迁移，随后删除 tenant/replay/cleanup 兼容代码。
3. 单独审计 IAM MQ topology，把通用命名/声明能力与 IAM 领域拓扑分开。
4. Todo 只有在出现第二个 tenant-schema 通用案例后，才重新评估是否抽象。

每个消费方必须可以通过回退 BOM/恢复本地实现完成独立回滚，不要求三方同步切换。

## 风险与回滚

- **重复投递仍可能发生**：broker 成功后进程在 CAS 前崩溃会重发。通过明确
  at-least-once 和 `eventId` 幂等契约控制。
- **Kingbase SQL 方言差异**：`FOR UPDATE SKIP LOCKED`、limit、时区类型必须用
  Finch Kingbase 集成测试验证，不能只用 H2 模拟。
- **错误 producer 配置**：启动时校验 producer；所有 claim、重放、清理均按
  producer 约束。
- **清理误删**：默认关闭自动清理，只删除 `PUBLISHED` 且每批有上限。
- **V1/V2 双写**：文档明确迁移期只能选择一个写入路径；独立表保证回滚不互相
  污染。
- **回滚**：由于 V2 默认关闭且使用独立表，可回退平台版本并保留 V2 表用于审计；
  V1 终态修复可独立回退，不涉及数据迁移。
