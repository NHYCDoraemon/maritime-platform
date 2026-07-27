package com.maritime.platform.common.security.config;

import com.maritime.platform.common.security.jwt.JwtProperties;
import com.maritime.platform.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class));

    @Test
    void registersJwtTokenProviderExplicitly() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(JwtTokenProvider.class));
    }

    @Test
    void backsOffForConsumerJwtTokenProvider() {
        JwtTokenProvider custom = new JwtTokenProvider(new JwtProperties());

        runner.withBean(JwtTokenProvider.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtTokenProvider.class);
                    assertThat(context.getBean(JwtTokenProvider.class))
                            .isSameAs(custom);
                });
    }
}
