package com.maritime.platform.common.mq.topology;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
    public TopicExchange permissionExchange() {
        return new TopicExchange(PERMISSION_EXCHANGE);
    }

    @Bean
    public TopicExchange syncExchange() {
        return new TopicExchange(SYNC_EXCHANGE);
    }

    @Bean
    public FanoutExchange cacheInvalidationExchange() {
        return new FanoutExchange(CACHE_INVALIDATION_EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    /**
     * Topic exchange for audit events consumed by the data platform.
     * IAM publishes; data platform subscribes for statistics/analytics.
     */
    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    // ---- Queue Beans ----

    @Bean
    public Queue permissionChangedQueue() {
        return QueueBuilder.durable(QUEUE_PERMISSION_CHANGED)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_PERMISSION_CHANGED_DLQ)
                .build();
    }

    @Bean
    public Queue cacheInvalidationQueue() {
        return QueueBuilder.durable(QUEUE_CACHE_INVALIDATION)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_CACHE_INVALIDATION_DLQ)
                .build();
    }

    @Bean
    public Queue syncOrgQueue() {
        return QueueBuilder.durable(QUEUE_SYNC_ORG)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_SYNC_ORG_DLQ)
                .build();
    }

    @Bean
    public Queue syncUserQueue() {
        return QueueBuilder.durable(QUEUE_SYNC_USER)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_SYNC_USER_DLQ)
                .build();
    }

    // ---- DLX Queue Beans ----

    @Bean
    public Queue permissionChangedDlq() {
        return QueueBuilder.durable(QUEUE_PERMISSION_CHANGED_DLQ).build();
    }

    @Bean
    public Queue cacheInvalidationDlq() {
        return QueueBuilder.durable(QUEUE_CACHE_INVALIDATION_DLQ).build();
    }

    @Bean
    public Queue syncOrgDlq() {
        return QueueBuilder.durable(QUEUE_SYNC_ORG_DLQ).build();
    }

    @Bean
    public Queue syncUserDlq() {
        return QueueBuilder.durable(QUEUE_SYNC_USER_DLQ).build();
    }

    // ---- Bindings ----

    @Bean
    public Binding permissionChangedBinding(Queue permissionChangedQueue,
                                            TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_PERMISSION_CHANGED);
    }

    @Bean
    public Binding positionRoleChangedBinding(Queue permissionChangedQueue,
                                              TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_POSITION_ROLE_CHANGED);
    }

    @Bean
    public Binding orgChangedBinding(Queue permissionChangedQueue,
                                     TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_ORG_CHANGED);
    }

    @Bean
    public Binding appEnabledBinding(Queue permissionChangedQueue,
                                     TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_APP_ENABLED);
    }

    @Bean
    public Binding userDismissedBinding(Queue permissionChangedQueue,
                                        TopicExchange permissionExchange) {
        return BindingBuilder.bind(permissionChangedQueue)
                .to(permissionExchange)
                .with(RK_USER_DISMISSED);
    }

    @Bean
    public Binding cacheInvalidationBinding(Queue cacheInvalidationQueue,
                                            FanoutExchange cacheInvalidationExchange) {
        return BindingBuilder.bind(cacheInvalidationQueue)
                .to(cacheInvalidationExchange);
    }

    @Bean
    public Binding syncOrgBinding(Queue syncOrgQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncOrgQueue).to(syncExchange).with(RK_SYNC_ORG);
    }

    @Bean
    public Binding syncUserBinding(Queue syncUserQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncUserQueue).to(syncExchange).with(RK_SYNC_USER);
    }

    // ---- DLX Bindings ----

    @Bean
    public Binding permissionChangedDlqBinding(Queue permissionChangedDlq,
                                               DirectExchange dlxExchange) {
        return BindingBuilder.bind(permissionChangedDlq)
                .to(dlxExchange)
                .with(QUEUE_PERMISSION_CHANGED_DLQ);
    }

    @Bean
    public Binding cacheInvalidationDlqBinding(Queue cacheInvalidationDlq,
                                               DirectExchange dlxExchange) {
        return BindingBuilder.bind(cacheInvalidationDlq)
                .to(dlxExchange)
                .with(QUEUE_CACHE_INVALIDATION_DLQ);
    }

    @Bean
    public Binding syncOrgDlqBinding(Queue syncOrgDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(syncOrgDlq)
                .to(dlxExchange)
                .with(QUEUE_SYNC_ORG_DLQ);
    }

    @Bean
    public Binding syncUserDlqBinding(Queue syncUserDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(syncUserDlq)
                .to(dlxExchange)
                .with(QUEUE_SYNC_USER_DLQ);
    }
}
