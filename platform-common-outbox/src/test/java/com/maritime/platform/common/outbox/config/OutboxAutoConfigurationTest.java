package com.maritime.platform.common.outbox.config;

import com.maritime.platform.common.outbox.mapper.OutboxEntryMapper;
import com.maritime.platform.common.outbox.poller.OutboxPoller;
import com.maritime.platform.common.outbox.spi.OutboxEventPublisher;
import com.maritime.platform.common.outbox.store.OutboxStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            OutboxAutoConfiguration.class))
                    .withBean(
                            OutboxEntryMapper.class,
                            () -> mock(OutboxEntryMapper.class))
                    .withBean(
                            OutboxEventPublisher.class,
                            () -> mock(OutboxEventPublisher.class));

    @Test
    void createsDefaultOutboxBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(OutboxStore.class);
            assertThat(context).hasSingleBean(OutboxPoller.class);
        });
    }

    @Test
    void backsOffForConsumerOutboxBeans() {
        OutboxStore store =
                new OutboxStore(mock(OutboxEntryMapper.class));
        OutboxPoller poller = mock(OutboxPoller.class);

        runner.withBean("consumerOutboxStore", OutboxStore.class, () -> store)
                .withBean("consumerOutboxPoller", OutboxPoller.class, () -> poller)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxStore.class);
                    assertThat(context).hasSingleBean(OutboxPoller.class);
                    assertThat(context.getBean(OutboxStore.class))
                            .isSameAs(store);
                    assertThat(context.getBean(OutboxPoller.class))
                            .isSameAs(poller);
                });
    }
}
