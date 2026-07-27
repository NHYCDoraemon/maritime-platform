package com.maritime.platform.common.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.maritime.platform.common.core.id.SnowflakeIdGenerator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MybatisPlusConfigAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusConfig.class))
            .withBean(SnowflakeIdGenerator.class,
                    () -> new SnowflakeIdGenerator(0, 0));

    @Test
    void createsDefaultInterceptor() {
        runner.run(context -> {
            assertThat(context)
                    .hasSingleBean(MybatisPlusInterceptor.class)
                    .hasSingleBean(MetaObjectHandler.class)
                    .hasSingleBean(ISqlInjector.class)
                    .hasSingleBean(IdentifierGenerator.class);
            assertThat(context.getBean(ISqlInjector.class))
                    .isInstanceOf(IamSqlInjector.class);
        });
    }

    @Test
    void appliesConsumerCustomizers() {
        AtomicBoolean customized = new AtomicBoolean();

        runner.withBean(
                        MybatisPlusInterceptorCustomizer.class,
                        () -> interceptor -> customized.set(true))
                .run(context -> assertThat(customized).isTrue());
    }

    @Test
    void backsOffForConsumerInterceptor() {
        MybatisPlusInterceptor custom = new MybatisPlusInterceptor();

        runner.withBean(MybatisPlusInterceptor.class, () -> custom)
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class))
                            .isSameAs(custom);
                });
    }

    @Test
    void backsOffForConsumerExtensionBeans() {
        MetaObjectHandler metaObjectHandler = mock(MetaObjectHandler.class);
        ISqlInjector sqlInjector = mock(ISqlInjector.class);
        IdentifierGenerator identifierGenerator =
                mock(IdentifierGenerator.class);

        runner.withBean(MetaObjectHandler.class, () -> metaObjectHandler)
                .withBean(ISqlInjector.class, () -> sqlInjector)
                .withBean(
                        IdentifierGenerator.class,
                        () -> identifierGenerator)
                .run(context -> {
                    assertThat(context.getBean(MetaObjectHandler.class))
                            .isSameAs(metaObjectHandler);
                    assertThat(context.getBean(ISqlInjector.class))
                            .isSameAs(sqlInjector);
                    assertThat(context.getBean(IdentifierGenerator.class))
                            .isSameAs(identifierGenerator);
                });
    }
}
