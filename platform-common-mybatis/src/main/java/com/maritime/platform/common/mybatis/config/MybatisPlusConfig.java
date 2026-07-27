package com.maritime.platform.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.maritime.platform.common.core.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus auto-configuration for the IAM platform.
 *
 * <p>Registers pagination, optimistic locking, auto-fill, batch insert,
 * and snowflake ID generation support.
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class MybatisPlusConfig {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            ObjectProvider<MybatisPlusInterceptorCustomizer> customizers) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInterceptor =
                new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(100L);
        interceptor.addInnerInterceptor(paginationInterceptor);

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        customizers.orderedStream()
                .forEach(customizer -> customizer.customize(interceptor));

        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler autoFillMetaHandler() {
        return new AutoFillMetaHandler();
    }

    @Bean
    @ConditionalOnMissingBean(ISqlInjector.class)
    public IamSqlInjector iamSqlInjector() {
        return new IamSqlInjector();
    }

    @Bean
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    public IdentifierGenerator snowflakeIdentifierGenerator(
            SnowflakeIdGenerator snowflakeIdGenerator) {
        return new SnowflakeIdentifierGenerator(snowflakeIdGenerator);
    }
}
