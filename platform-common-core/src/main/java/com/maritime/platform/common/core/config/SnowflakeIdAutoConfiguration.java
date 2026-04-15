package com.maritime.platform.common.core.config;

import com.maritime.platform.common.core.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SnowflakeIdAutoConfiguration {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${iam.snowflake.datacenter-id:0}") long datacenterId,
            @Value("${iam.snowflake.worker-id:0}") long workerId) {
        return new SnowflakeIdGenerator(datacenterId, workerId);
    }
}
