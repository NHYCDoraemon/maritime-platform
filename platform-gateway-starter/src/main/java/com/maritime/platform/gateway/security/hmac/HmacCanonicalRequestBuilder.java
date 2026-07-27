package com.maritime.platform.gateway.security.hmac;


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
 * <p>The canonical form is linefeed-delimited with field name labels:
 * <pre>
 * appKey={appKey}
 * method={HTTP_METHOD}
 * path={rawPath}
 * query={canonicalQuery}
 * timestamp={timestamp}
 * nonce={nonce}
 * bodyDigest={sha256Hex(body)}
 * </pre>
 * Query parameters in canonicalQuery are sorted by key then value.
 * Values are URL-encoded (space as {@code %20}, not {@code +}).
 * Timestamp is epoch millis. bodyDigest is SHA-256 hex of the raw request body.
 * The final signature is {@code HMAC-SHA256(appSecret, canonicalString)} as lowercase hex.
 */
public class HmacCanonicalRequestBuilder {

	private static final String LF = "\n";

	public String build(String appKey, String method, String rawPath, String rawQuery,
			String timestamp, String nonce, String bodyDigest) {
		StringBuilder sb = new StringBuilder(512);
		sb.append("appKey=").append(appKey).append(LF);
		sb.append("method=").append(method.toUpperCase()).append(LF);
		sb.append("path=").append(rawPath != null ? rawPath : "").append(LF);
		sb.append("query=").append(canonicalizeQuery(rawQuery)).append(LF);
		sb.append("timestamp=").append(timestamp).append(LF);
		sb.append("nonce=").append(nonce).append(LF);
		sb.append("bodyDigest=").append(bodyDigest);
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
