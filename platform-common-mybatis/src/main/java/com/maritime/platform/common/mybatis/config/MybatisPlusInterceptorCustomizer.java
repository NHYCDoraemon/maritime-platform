package com.maritime.platform.common.mybatis.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;

/**
 * Callback for adding or adjusting inner interceptors on the platform default
 * {@link MybatisPlusInterceptor}.
 */
@FunctionalInterface
public interface MybatisPlusInterceptorCustomizer {

    void customize(MybatisPlusInterceptor interceptor);
}
