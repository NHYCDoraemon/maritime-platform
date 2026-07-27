package com.maritime.iam.sdk;

import com.maritime.iam.sdk.client.IamQueryClient;
import com.maritime.iam.sdk.mapper.ApiToPageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IamSdkAutoConfigurationTest {

    private final WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            IamSdkAutoConfiguration.class))
                    .withBean(
                            "requestMappingHandlerMapping",
                            RequestMappingHandlerMapping.class,
                            () -> mock(RequestMappingHandlerMapping.class))
                    .withBean(
                            ApiToPageMapper.class,
                            () -> mock(ApiToPageMapper.class))
                    .withPropertyValues(
                            "iam.center.url=http://iam-query:9083",
                            "iam.app.code=PROCESS",
                            "iam.app.secret=test-secret");

    @Test
    void usesConsumerIamSdkRestTemplate() {
        RestTemplate custom = new RestTemplate();

        runner.withBean(
                        "iamSdkRestTemplate",
                        RestTemplate.class,
                        () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(IamQueryClient.class);
                    IamQueryClient client =
                            context.getBean(IamQueryClient.class);
                    assertThat(ReflectionTestUtils.getField(
                            client, "restTemplate")).isSameAs(custom);
                });
    }
}
