package com.maritime.platform.gateway.security.hmac;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the canonical string used for HMAC signature computation.
 *
 * <p>The canonical form is linefeed-delimited:
 * <pre>
 * {appKey}
 * {METHOD}
 * {rawPath}
 * {canonicalQuery}
 * {timestamp}
 * {nonce}
 * {bodyDigest}
 * </pre>
 * Query parameters in canonicalQuery are sorted by key then value.
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.hmac.enabled")
public class HmacCanonicalRequestBuilder {

	private static final String LF = "\n";

	public String build(String appKey, String method, String rawPath, String rawQuery,
			String timestamp, String nonce, String bodyDigest) {
		StringBuilder sb = new StringBuilder(512);
		sb.append(appKey).append(LF);
		sb.append(method.toUpperCase()).append(LF);
		sb.append(rawPath != null ? rawPath : "").append(LF);
		sb.append(canonicalizeQuery(rawQuery)).append(LF);
		sb.append(timestamp).append(LF);
		sb.append(nonce).append(LF);
		sb.append(bodyDigest);
		return sb.toString();
	}

	String canonicalizeQuery(String rawQuery) {
		if (rawQuery == null || rawQuery.isEmpty()) {
			return "";
		}
		Map<String, String[]> params = new LinkedHashMap<>();
		for (String pair : rawQuery.split("&")) {
			int idx = pair.indexOf('=');
			String key;
			String value;
			if (idx >= 0) {
				key = decode(pair.substring(0, idx));
				value = decode(pair.substring(idx + 1));
			} else {
				key = decode(pair);
				value = "";
			}
			params.merge(key, new String[]{value}, (existing, incoming) -> {
				String[] merged = Arrays.copyOf(existing, existing.length + 1);
				merged[existing.length] = incoming[0];
				return merged;
			});
		}
		return params.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.flatMap(e -> Arrays.stream(e.getValue())
						.map(v -> encode(e.getKey()) + "=" + encode(v)))
				.collect(Collectors.joining("&"));
	}

	private static String decode(String s) {
		return URLDecoder.decode(s, StandardCharsets.UTF_8);
	}

	private static String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
