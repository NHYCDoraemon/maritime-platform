package com.maritime.platform.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Normalizes client-supplied trace IDs according to the platform TraceId contract.
 * <p>
 * Valid trace IDs contain only ASCII letters, digits, hyphens, underscores, and dots,
 * with length between 1 and 128 characters. Invalid, blank, or absent values are
 * replaced with a generated 32-character hex UUID.
 */
public final class TraceIdNormalizer {

    private static final Pattern VALID_PATTERN = Pattern.compile("[a-zA-Z0-9\\-_.]{1,128}");

    private static final int MAX_LENGTH = 128;

    private TraceIdNormalizer() {
    }

    /**
     * Normalize a raw trace ID value.
     *
     * @param raw the raw value from the client header, may be null
     * @return the original value if valid, or a generated 32-char hex UUID otherwise
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return generate();
        }
        if (raw.length() > MAX_LENGTH) {
            return generate();
        }
        if (!VALID_PATTERN.matcher(raw).matches()) {
            return generate();
        }
        return raw;
    }

    private static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
