package com.maritime.platform.gateway.security.hmac;

import reactor.core.publisher.Mono;

/**
 * Resolves {@link AppCredential} from an appKey.
 *
 * <p>Implementations may read from Redis, a configuration source,
 * or an external IAM service. The default implementation uses
 * Redis with a configuration fallback.
 */
@FunctionalInterface
public interface AppCredentialResolver {

	/**
	 * Resolve the credential for the given app key.
	 *
	 * @param appKey the app key to look up
	 * @return Mono of AppCredential, or an error Mono with
	 *         {@code UNKNOWN_APP} or {@code APP_DISABLED}
	 */
	Mono<AppCredential> resolve(String appKey);
}
