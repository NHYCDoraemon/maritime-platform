package com.maritime.platform.gateway.autoconfigure;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.filter.ContextHeaderInjectionFilter;
import com.maritime.platform.gateway.filter.HmacAuthenticationGatewayFilter;
import com.maritime.platform.gateway.filter.JwtAuthenticationGatewayFilter;
import com.maritime.platform.gateway.filter.JwtNonceGatewayFilter;
import com.maritime.platform.gateway.filter.RequestLogGatewayFilter;
import com.maritime.platform.gateway.filter.RouteSecurityPolicyFilter;
import com.maritime.platform.gateway.filter.TraceIdGatewayFilter;
import com.maritime.platform.gateway.filter.TrustedHeaderWriter;
import com.maritime.platform.gateway.filter.UntrustedHeaderStripFilter;
import com.maritime.platform.gateway.security.GatewayPrincipalHeaderCustomizer;
import com.maritime.platform.gateway.security.GatewaySecurityPolicyCustomizer;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicyResolver;
import com.maritime.platform.gateway.security.hmac.AppCredentialResolver;
import com.maritime.platform.gateway.security.hmac.DefaultAppCredentialResolver;
import com.maritime.platform.gateway.security.hmac.HmacAuthenticationManager;
import com.maritime.platform.gateway.security.hmac.HmacCanonicalRequestBuilder;
import com.maritime.platform.gateway.security.hmac.HmacNonceValidator;
import com.maritime.platform.gateway.security.jwt.DefaultJwtClaimsMapper;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationManager;
import com.maritime.platform.gateway.security.jwt.JwtClaimsMapper;
import com.maritime.platform.gateway.security.jwt.JwtStateValidator;
import com.maritime.platform.gateway.security.nonce.JwtNonceValidator;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@AutoConfiguration(before = org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration")
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GatewayErrorWriter gatewayErrorWriter() {
		return new DefaultGatewayErrorWriter();
	}

	@Bean
	TrustedHeaderWriter trustedHeaderWriter() {
		return new TrustedHeaderWriter();
	}

	@Bean
	RouteSecurityPolicyResolver routeSecurityPolicyResolver(
			GatewaySecurityProperties properties,
			ObjectProvider<GatewaySecurityPolicyCustomizer> customizers) {
		List<GatewaySecurityPolicyCustomizer> orderedCustomizers =
				customizers.orderedStream().toList();
		return new RouteSecurityPolicyResolver(properties, orderedCustomizers);
	}

	@Bean
	TraceIdGatewayFilter traceIdGatewayFilter() {
		return new TraceIdGatewayFilter();
	}

	@Bean
	UntrustedHeaderStripFilter untrustedHeaderStripFilter(
			GatewaySecurityProperties properties) {
		return new UntrustedHeaderStripFilter(properties);
	}

	@Bean
	RouteSecurityPolicyFilter routeSecurityPolicyFilter(
			RouteSecurityPolicyResolver resolver) {
		return new RouteSecurityPolicyFilter(resolver);
	}

	@Bean
	ContextHeaderInjectionFilter contextHeaderInjectionFilter(
			TrustedHeaderWriter trustedHeaderWriter,
			GatewaySecurityProperties properties,
			ObjectProvider<GatewayPrincipalHeaderCustomizer> customizers) {
		return new ContextHeaderInjectionFilter(
				trustedHeaderWriter,
				properties,
				customizers.orderedStream().toList());
	}

	@Bean
	RequestLogGatewayFilter requestLogGatewayFilter() {
		return new RequestLogGatewayFilter();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.jwt",
			name = "enabled",
			havingValue = "true")
	JwtClaimsMapper jwtClaimsMapper(GatewaySecurityProperties properties) {
		return new DefaultJwtClaimsMapper(properties);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.jwt",
			name = "enabled",
			havingValue = "true")
	JwtStateValidator jwtStateValidator(
			ReactiveRedisOperations<String, String> redisOperations,
			GatewaySecurityProperties properties) {
		return new JwtStateValidator(redisOperations, properties);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.jwt",
			name = "enabled",
			havingValue = "true")
	JwtAuthenticationManager jwtAuthenticationManager(
			GatewaySecurityProperties properties,
			JwtClaimsMapper claimsMapper,
			JwtStateValidator stateValidator) {
		return new JwtAuthenticationManager(
				properties, claimsMapper, stateValidator);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.jwt",
			name = "enabled",
			havingValue = "true")
	JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter(
			JwtAuthenticationManager authenticationManager,
			GatewayErrorWriter errorWriter) {
		return new JwtAuthenticationGatewayFilter(
				authenticationManager, errorWriter);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.jwt",
			name = "enabled",
			havingValue = "true")
	JwtNonceValidator jwtNonceValidator(
			ReactiveRedisOperations<String, String> redisOperations,
			GatewaySecurityProperties properties) {
		return new JwtNonceValidator(redisOperations, properties);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.jwt",
			name = "enabled",
			havingValue = "true")
	JwtNonceGatewayFilter jwtNonceGatewayFilter(
			JwtNonceValidator nonceValidator,
			GatewaySecurityProperties properties,
			GatewayErrorWriter errorWriter) {
		return new JwtNonceGatewayFilter(
				nonceValidator, properties, errorWriter);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.hmac",
			name = "enabled",
			havingValue = "true")
	AppCredentialResolver appCredentialResolver(
			GatewaySecurityProperties properties,
			ObjectProvider<ReactiveRedisTemplate<String, String>> redisTemplate) {
		return new DefaultAppCredentialResolver(
				properties, redisTemplate.getIfAvailable());
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.hmac",
			name = "enabled",
			havingValue = "true")
	HmacCanonicalRequestBuilder hmacCanonicalRequestBuilder() {
		return new HmacCanonicalRequestBuilder();
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.hmac",
			name = "enabled",
			havingValue = "true")
	HmacNonceValidator hmacNonceValidator(
			ReactiveRedisOperations<String, String> redisOperations,
			GatewaySecurityProperties properties) {
		return new HmacNonceValidator(redisOperations, properties);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.hmac",
			name = "enabled",
			havingValue = "true")
	HmacAuthenticationManager hmacAuthenticationManager(
			GatewaySecurityProperties properties,
			HmacCanonicalRequestBuilder canonicalRequestBuilder,
			HmacNonceValidator nonceValidator,
			AppCredentialResolver credentialResolver) {
		return new HmacAuthenticationManager(
				properties,
				canonicalRequestBuilder,
				nonceValidator,
				credentialResolver);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "maritime.gateway.security.hmac",
			name = "enabled",
			havingValue = "true")
	HmacAuthenticationGatewayFilter hmacAuthenticationGatewayFilter(
			HmacAuthenticationManager authenticationManager,
			GatewaySecurityProperties properties,
			GatewayErrorWriter errorWriter) {
		return new HmacAuthenticationGatewayFilter(
				authenticationManager, properties, errorWriter);
	}
}
