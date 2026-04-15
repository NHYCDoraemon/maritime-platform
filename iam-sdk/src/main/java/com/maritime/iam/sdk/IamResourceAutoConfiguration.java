package com.maritime.iam.sdk;

import com.maritime.iam.sdk.resource.NacosResourcePublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Standalone auto-configuration for resource auto-registration.
 * Independent from {@link IamSdkAutoConfiguration} — does NOT
 * require {@code iam.center.url}. Only needs:
 * {@code iam.resource.auto-register=true} + nacos-client on classpath.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "iam.resource",
        name = "auto-register",
        havingValue = "true")
@ConditionalOnClass(name = "com.alibaba.nacos.api.NacosFactory")
public class IamResourceAutoConfiguration {

    @Bean
    NacosResourcePublisher nacosResourcePublisher() {
        return new NacosResourcePublisher();
    }
}
