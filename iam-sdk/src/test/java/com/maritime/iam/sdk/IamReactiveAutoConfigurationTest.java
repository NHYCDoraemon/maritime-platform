package com.maritime.iam.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.maritime.iam.sdk.permission.ReactiveIamPermissionClient;
import com.maritime.iam.sdk.permission.ReactivePermissionCodeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner
        .ReactiveWebApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

class IamReactiveAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner runner =
            new ReactiveWebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            IamReactiveAutoConfiguration.class))
                    .withBean(
                            ReactiveStringRedisTemplate.class,
                            () -> mock(
                                    ReactiveStringRedisTemplate.class))
                    .withPropertyValues(
                            "iam.center.url=http://iam-query:9083",
                            "iam.app.code=TODO");

    @Test
    void configuresReactivePermissionProvider() {
        runner.run(context -> {
            assertThat(context)
                    .hasSingleBean(ReactiveIamPermissionClient.class);
            assertThat(context)
                    .hasSingleBean(
                            ReactivePermissionCodeProvider.class);
            assertThat(context)
                    .hasBean("iamSdkReactiveWebClient");
        });
    }
}
