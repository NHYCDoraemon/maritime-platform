package com.maritime.platform.common.web.filter;

import com.maritime.platform.common.security.context.SecurityContextHolder;
import com.maritime.platform.common.security.context.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts gateway-injected user headers and populates
 * {@link SecurityContextHolder} for downstream services.
 *
 * <p>Gateway's JwtAuthGlobalFilter injects these headers after
 * JWT verification:
 * <ul>
 *   <li>{@code X-User-Id}</li>
 *   <li>{@code X-User-Name} (URL-encoded)</li>
 *   <li>{@code X-Active-Org-Code}</li>
 *   <li>{@code X-Session-Id}</li>
 *   <li>{@code X-System-Scope} (comma-separated)</li>
 * </ul>
 *
 * <p>Runs before business interceptors (order = Ordered.HIGHEST_PRECEDENCE + 5).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class GatewayUserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_ACTIVE_ORG_CODE =
            "X-Active-Org-Code";
    private static final String HEADER_SESSION_ID = "X-Session-Id";
    private static final String HEADER_SYSTEM_SCOPE =
            "X-System-Scope";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader(HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            String userName = urlDecode(
                    request.getHeader(HEADER_USER_NAME));
            String activeOrgCode =
                    request.getHeader(HEADER_ACTIVE_ORG_CODE);
            String sessionId =
                    request.getHeader(HEADER_SESSION_ID);
            List<String> systemScope = parseScope(
                    request.getHeader(HEADER_SYSTEM_SCOPE));

            SecurityContextHolder.set(new SecurityUser(
                    userId, userName, activeOrgCode,
                    systemScope, sessionId));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private static String urlDecode(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static List<String> parseScope(String scopeHeader) {
        if (scopeHeader == null || scopeHeader.isBlank()) {
            return List.of();
        }
        return Arrays.asList(scopeHeader.split(","));
    }
}
