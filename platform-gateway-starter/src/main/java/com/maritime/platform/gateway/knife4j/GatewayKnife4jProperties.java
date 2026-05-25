package com.maritime.platform.gateway.knife4j;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maritime.gateway.knife4j")
public class GatewayKnife4jProperties {

    private boolean enabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
