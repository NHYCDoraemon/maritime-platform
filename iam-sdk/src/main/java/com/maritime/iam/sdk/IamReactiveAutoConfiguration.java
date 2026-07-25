package com.maritime.iam.sdk;

import com.maritime.iam.sdk.permission.ReactiveIamPermissionClient;
import com.maritime.iam.sdk.permission.ReactivePermissionCodeProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Reactive IAM SDK auto-configuration for Spring Cloud Gateway.
 */
@AutoConfiguration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({
        WebClient.class,
        ReactiveStringRedisTemplate.class
})
@ConditionalOnProperty(prefix = "iam.center", name = "url")
@EnableConfigurationProperties(IamSdkProperties.class)
public class IamReactiveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "iamSdkReactiveWebClient")
    WebClient iamSdkReactiveWebClient(
            IamSdkProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getCenter().getUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ReactiveIamPermissionClient reactiveIamPermissionClient(
            @Qualifier("iamSdkReactiveWebClient")
            WebClient iamSdkReactiveWebClient) {
        return new ReactiveIamPermissionClient(
                iamSdkReactiveWebClient);
    }

    @Bean
    @ConditionalOnMissingBean
    ReactivePermissionCodeProvider reactivePermissionCodeProvider(
            ReactiveIamPermissionClient client,
            ReactiveStringRedisTemplate redisTemplate,
            IamSdkProperties properties) {
        IamSdkProperties.Cache cache =
                properties.getSdk().getCache();
        return new ReactivePermissionCodeProvider(
                client,
                redisTemplate,
                properties.getApp().getCode(),
                cache.getPermissionCodesTtl(),
                cache.getEmptyPermissionCodesTtl(),
                cache.getVersionCheckInterval(),
                properties.getSdk().isFailOpen(),
                cache.getMaxPermissionHeaderBytes());
    }
}
