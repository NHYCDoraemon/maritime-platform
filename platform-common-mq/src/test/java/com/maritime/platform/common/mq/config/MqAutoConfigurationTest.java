package com.maritime.platform.common.mq.config;

import com.maritime.platform.common.mq.topology.IamTopologyConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MqAutoConfigurationTest {

    @Test
    void enablesLegacyIamTopologyByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IamTopologyConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("permissionExchange");
                    assertThat(context).hasBean("permissionChangedQueue");
                    assertThat(context).hasBean("permissionChangedBinding");
                });
    }

    @Test
    void backsOffForNamedIamTopologyBean() {
        TopicExchange custom =
                new TopicExchange("consumer.permission.exchange");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IamTopologyConfiguration.class))
                .withBean("permissionExchange", TopicExchange.class,
                        () -> custom)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("permissionExchange"))
                            .isSameAs(custom);
                });
    }

    @Test
    void disablesLegacyIamTopologyExplicitly() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IamTopologyConfiguration.class))
                .withPropertyValues(
                        "maritime.mq.iam-topology.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(TopicExchange.class));
    }

    @Test
    void backsOffForConsumerMessageConverter() {
        Jackson2JsonMessageConverter custom =
                new Jackson2JsonMessageConverter();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        RabbitMQAutoConfiguration.class))
                .withBean(
                        Jackson2JsonMessageConverter.class,
                        () -> custom)
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(Jackson2JsonMessageConverter.class);
                    assertThat(context.getBean(
                            Jackson2JsonMessageConverter.class))
                            .isSameAs(custom);
                });
    }
}
