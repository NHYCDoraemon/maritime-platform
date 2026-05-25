package com.maritime.platform.gateway.security.hmac;

import java.util.List;

/**
 * Platform-neutral credential resolved from an appKey.
 * Used by the HMAC manager for signature verification and principal construction.
 */
public class AppCredential {

	private final String appKey;
	private final String appSecret;
	private final String appCode;
	private final String appId;
	private final String tenantId;
	private final String tenantCode;
	private final List<String> permissions;
	private final boolean enabled;

	private AppCredential(Builder builder) {
		this.appKey = builder.appKey;
		this.appSecret = builder.appSecret;
		this.appCode = builder.appCode;
		this.appId = builder.appId;
		this.tenantId = builder.tenantId;
		this.tenantCode = builder.tenantCode;
		this.permissions = builder.permissions != null ? List.copyOf(builder.permissions) : List.of();
		this.enabled = builder.enabled;
	}

	public String getAppKey() { return appKey; }
	public String getAppSecret() { return appSecret; }
	public String getAppCode() { return appCode; }
	public String getAppId() { return appId; }
	public String getTenantId() { return tenantId; }
	public String getTenantCode() { return tenantCode; }
	public List<String> getPermissions() { return permissions; }
	public boolean isEnabled() { return enabled; }

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String appKey;
		private String appSecret;
		private String appCode;
		private String appId;
		private String tenantId;
		private String tenantCode;
		private List<String> permissions;
		private boolean enabled = true;

		public Builder appKey(String appKey) { this.appKey = appKey; return this; }
		public Builder appSecret(String appSecret) { this.appSecret = appSecret; return this; }
		public Builder appCode(String appCode) { this.appCode = appCode; return this; }
		public Builder appId(String appId) { this.appId = appId; return this; }
		public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
		public Builder tenantCode(String tenantCode) { this.tenantCode = tenantCode; return this; }
		public Builder permissions(List<String> permissions) { this.permissions = permissions; return this; }
		public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

		public AppCredential build() {
			return new AppCredential(this);
		}
	}
}
