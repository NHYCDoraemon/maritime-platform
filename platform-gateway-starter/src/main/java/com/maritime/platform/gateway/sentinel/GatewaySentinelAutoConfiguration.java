package com.maritime.platform.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.ServerResponse;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter")
@ConditionalOnProperty(name = "maritime.gateway.sentinel.enabled", havingValue = "true")
@EnableConfigurationProperties(GatewaySentinelProperties.class)
public class GatewaySentinelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ServerCodecConfigurer.class)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler(
            ServerCodecConfigurer serverCodecConfigurer) {
        return new SentinelGatewayBlockExceptionHandler(
                Collections.emptyList(), serverCodecConfigurer);
    }

    @PostConstruct
    public void init() {
        BlockRequestHandler blockRequestHandler = (exchange, t) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
            body.put("message", "FLOW_LIMITING");
            body.put("data", null);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        };
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
