package com.maritime.iam.sdk.permission;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Versioned permission-code result for trusted gateway headers.
 */
public record PermissionCodeSnapshot(
        String version,
        List<String> codes
) {

    public PermissionCodeSnapshot {
        version = version == null || version.isBlank()
                ? "0.0" : version;
        codes = codes == null ? List.of() : List.copyOf(codes);
    }

    /**
     * Parse IAM's comma-separated permission-code response.
     */
    public static PermissionCodeSnapshot parse(
            String version, String value) {
        if (value == null || value.isBlank()) {
            return new PermissionCodeSnapshot(version, List.of());
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .forEach(unique::add);
        return new PermissionCodeSnapshot(
                version, List.copyOf(unique));
    }

    /**
     * Serialize for the X-App-Permissions trusted header.
     */
    public String headerValue() {
        return String.join(",", codes);
    }

    /**
     * UTF-8 size of the trusted header value.
     */
    public int headerBytes() {
        return headerValue().getBytes(StandardCharsets.UTF_8).length;
    }
}
