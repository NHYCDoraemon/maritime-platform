package com.maritime.platform.gateway.sentinel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maritime.gateway.sentinel")
public class GatewaySentinelProperties {

    private boolean enabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
