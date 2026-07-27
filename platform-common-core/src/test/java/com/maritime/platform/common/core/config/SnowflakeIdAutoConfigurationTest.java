package com.maritime.platform.common.core.config;

import com.maritime.platform.common.core.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowflakeIdAutoConfiguration.class));

    @Test
    void createsDefaultGenerator() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(SnowflakeIdGenerator.class));
    }

    @Test
    void backsOffForConsumerGenerator() {
        SnowflakeIdGenerator custom = new SnowflakeIdGenerator(1, 2);

        runner.withBean(SnowflakeIdGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(SnowflakeIdGenerator.class);
                    assertThat(context.getBean(SnowflakeIdGenerator.class))
                            .isSameAs(custom);
                });
    }
}
