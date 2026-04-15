package com.maritime.platform.common.mybatis.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.maritime.platform.common.core.id.SnowflakeIdGenerator;

/**
 * Adapts {@link SnowflakeIdGenerator} to the MyBatis-Plus {@link IdentifierGenerator} contract.
 */
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SnowflakeIdentifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public Number nextId(Object entity) {
        return snowflakeIdGenerator.nextId();
    }
}
