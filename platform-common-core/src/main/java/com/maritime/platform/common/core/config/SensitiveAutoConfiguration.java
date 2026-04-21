package com.maritime.platform.common.core.config;

import com.maritime.platform.common.core.security.AesGcmSensitiveFieldEncryptor;
import com.maritime.platform.common.core.security.EnvKeyProvider;
import com.maritime.platform.common.core.security.KeyProvider;
import com.maritime.platform.common.core.security.SensitiveFieldEncryptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for sensitive field encryption.
 *
 * <p>Registers {@link EnvKeyProvider} and {@link AesGcmSensitiveFieldEncryptor}
 * beans unless the application provides its own overrides. Disable by setting
 * {@code platform.sensitive.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "platform.sensitive",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SensitiveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeyProvider keyProvider() {
        return new EnvKeyProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveFieldEncryptor sensitiveFieldEncryptor(KeyProvider keyProvider) {
        return new AesGcmSensitiveFieldEncryptor(keyProvider);
    }
}