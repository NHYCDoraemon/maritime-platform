package com.maritime.platform.gateway.security;

/**
 * Resolved security policy for a route, returned by {@link RouteSecurityPolicyResolver}.
 */
public class RouteSecurityPolicy {

	private final AuthMode authMode;

	private final String matchedRuleId;

	public RouteSecurityPolicy(AuthMode authMode, String matchedRuleId) {
		this.authMode = authMode;
		this.matchedRuleId = matchedRuleId;
	}

	public AuthMode getAuthMode() {
		return authMode;
	}

	public String getMatchedRuleId() {
		return matchedRuleId;
	}

	@Override
	public String toString() {
		return authMode + " [" + matchedRuleId + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RouteSecurityPolicy that)) return false;
		return authMode == that.authMode && matchedRuleId.equals(that.matchedRuleId);
	}

	@Override
	public int hashCode() {
		return 31 * authMode.hashCode() + matchedRuleId.hashCode();
	}
}
