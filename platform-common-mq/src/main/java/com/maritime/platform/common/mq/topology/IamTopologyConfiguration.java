package com.maritime.platform.common.mq.topology;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Legacy IAM-specific RabbitMQ topology.
 *
 * @deprecated IAM topology is a domain contract and will move out of
 *     {@code platform-common-mq} in the next major version. Non-IAM consumers
 *     should set {@code maritime.mq.iam-topology.enabled=false}.
 */
@Deprecated(since = "1.0.11", forRemoval = true)
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "maritime.mq.iam-topology",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class IamTopologyConfiguration {

    // ---- Exchanges ----

    public static final String PERMISSION_EXCHANGE = "iam.permission.exchange";
    public static final String SYNC_EXCHANGE = "iam.sync.exchange";
    public static final String CACHE_INVALIDATION_EXCHANGE = "iam.cache.invalidation.exchange";
    public static final String DLX_EXCHANGE = "iam.dlx.exchange";

    /** Audit events for data platform consumption (statistics, analytics). */
    public static final String AUDIT_EXCHANGE = "iam.audit.exchange";

    // ---- Queues ----

    public static final String QUEUE_PERMISSION_CHANGED = "queue.iam.permission.changed";
    public static final String QUEUE_CACHE_INVALIDATION = "queue.iam.cache.invalidation";
    public static final String QUEUE_SYNC_ORG = "queue.iam.sync.org";
    public static final String QUEUE_SYNC_USER = "queue.iam.sync.user";

    // ---- DLX Queues ----

    public static final String QUEUE_PERMISSION_CHANGED_DLQ = "queue.iam.permission.changed.dlq";
    public static final String QUEUE_CACHE_INVALIDATION_DLQ = "queue.iam.cache.invalidation.dlq";
    public static final String QUEUE_SYNC_ORG_DLQ = "queue.iam.sync.org.dlq";
    public static final String QUEUE_SYNC_USER_DLQ = "queue.iam.sync.user.dlq";

    // ---- Routing Keys ----

    public static final String RK_PERMISSION_CHANGED = "permission.changed";
    public static final String RK_POSITION_ROLE_CHANGED = "permission.position-role.changed";
    public static final String RK_ORG_CHANGED = "permission.org.changed";
    public static final String RK_APP_ENABLED = "permission.app.enabled";
    public static final String RK_USER_DISMISSED = "permission.user.dismissed";
    public static final String RK_SYNC_ORG = "sync.org";
    public static final String RK_SYNC_USER = "sync.user";

    // ---- Exchange Beans ----

    @Bean
    @ConditionalOnMissingBean(name = "permissionExchange")
    public TopicExchange permissionExchange() {
        return new TopicExchange(PERMISSION_EXCHANGE);
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncExchange")
    public TopicExchange syncExchange() {
        return new TopicExchange(SYNC_EXCHANGE);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cacheInvalidationExchange")
    public FanoutExchange cacheInvalidationExchange() {
        return new FanoutExchange(CACHE_INVALIDATION_EXCHANGE);
    }

    @Bean
    @ConditionalOnMissingBean(name = "dlxExchange")
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    /**
     * Topic exchange for audit events consumed by the data platform.
     * IAM publishes; data platform subscribes for statistics/analytics.
     */
    @Bean
    @ConditionalOnMissingBean(name = "auditExchange")
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    // ---- Queue Beans ----

    @Bean
    @ConditionalOnMissingBean(name = "permissionChangedQueue")
    public Queue permissionChangedQueue() {
        return QueueBuilder.durable(QUEUE_PERMISSION_CHANGED)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_PERMISSION_CHANGED_DLQ)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "cacheInvalidationQueue")
    public Queue cacheInvalidationQueue() {
        return QueueBuilder.durable(QUEUE_CACHE_INVALIDATION)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_CACHE_INVALIDATION_DLQ)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncOrgQueue")
    public Queue syncOrgQueue() {
        return QueueBuilder.durable(QUEUE_SYNC_ORG)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_SYNC_ORG_DLQ)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncUserQueue")
    public Queue syncUserQueue() {
        return QueueBuilder.durable(QUEUE_SYNC_USER)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_SYNC_USER_DLQ)
                .build();
    }

    // ---- DLX Queue Beans ----

    @Bean
    @ConditionalOnMissingBean(name = "permissionChangedDlq")
    public Queue permissionChangedDlq() {
        return QueueBuilder.durable(QUEUE_PERMISSION_CHANGED_DLQ).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "cacheInvalidationDlq")
    public Queue cacheInvalidationDlq() {
        return QueueBuilder.durable(QUEUE_CACHE_INVALIDATION_DLQ).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncOrgDlq")
    public Queue syncOrgDlq() {
        return QueueBuilder.durable(QUEUE_SYNC_ORG_DLQ).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncUserDlq")
    public Queue syncUserDlq() {
        return QueueBuilder.durable(QUEUE_SYNC_USER_DLQ).build();
    }

    // ---- Bindings ----

    @Bean
    @ConditionalOnMissingBean(name = "permissionChangedBinding")
    public Binding permissionChangedBinding(Queue permissionChangedQueue,
                                            TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_PERMISSION_CHANGED);
    }

    @Bean
    @ConditionalOnMissingBean(name = "positionRoleChangedBinding")
    public Binding positionRoleChangedBinding(Queue permissionChangedQueue,
                                              TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_POSITION_ROLE_CHANGED);
    }

    @Bean
    @ConditionalOnMissingBean(name = "orgChangedBinding")
    public Binding orgChangedBinding(Queue permissionChangedQueue,
                                     TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_ORG_CHANGED);
    }

    @Bean
    @ConditionalOnMissingBean(name = "appEnabledBinding")
    public Binding appEnabledBinding(Queue permissionChangedQueue,
                                     TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_APP_ENABLED);
    }

    @Bean
    @ConditionalOnMissingBean(name = "userDismissedBinding")
    public Binding userDismissedBinding(Queue permissionChangedQueue,
                                        TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_USER_DISMISSED);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cacheInvalidationBinding")
    public Binding cacheInvalidationBinding(Queue cacheInvalidationQueue,
                                            FanoutExchange cacheInvalidationExchange) {
        return BindingBuilder.bind(cacheInvalidationQueue)
                .to(cacheInvalidationExchange);
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncOrgBinding")
    public Binding syncOrgBinding(Queue syncOrgQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncOrgQueue).to(syncExchange).with(RK_SYNC_ORG);
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncUserBinding")
    public Binding syncUserBinding(Queue syncUserQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncUserQueue).to(syncExchange).with(RK_SYNC_USER);
    }

    // ---- DLX Bindings ----

    @Bean
    @ConditionalOnMissingBean(name = "permissionChangedDlqBinding")
    public Binding permissionChangedDlqBinding(Queue permissionChangedDlq,
                                               DirectExchange dlxExchange) {
        return BindingBuilder.bind(permissionChangedDlq)
                .to(dlxExchange)
                .with(QUEUE_PERMISSION_CHANGED_DLQ);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cacheInvalidationDlqBinding")
    public Binding cacheInvalidationDlqBinding(Queue cacheInvalidationDlq,
                                               DirectExchange dlxExchange) {
        return BindingBuilder.bind(cacheInvalidationDlq)
                .to(dlxExchange)
                .with(QUEUE_CACHE_INVALIDATION_DLQ);
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncOrgDlqBinding")
    public Binding syncOrgDlqBinding(Queue syncOrgDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(syncOrgDlq)
                .to(dlxExchange)
                .with(QUEUE_SYNC_ORG_DLQ);
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncUserDlqBinding")
    public Binding syncUserDlqBinding(Queue syncUserDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(syncUserDlq)
                .to(dlxExchange)
                .with(QUEUE_SYNC_USER_DLQ);
    }
}
