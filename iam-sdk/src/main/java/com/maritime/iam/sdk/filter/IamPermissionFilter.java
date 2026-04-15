package com.maritime.iam.sdk.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritime.platform.common.security.annotation.PublicApi;
import com.maritime.platform.common.security.annotation.RequirePermission;
import com.maritime.iam.sdk.IamSdkProperties;
import com.maritime.iam.sdk.client.IamQueryClient;
import com.maritime.iam.sdk.context.IamContext;
import com.maritime.iam.sdk.mapper.ApiToPageMapper;
import com.maritime.iam.sdk.model.NavSnapshot;
import com.maritime.iam.sdk.model.PageSnapshot;
import com.maritime.iam.sdk.response.IamResponse403;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Servlet Filter implementing default-deny permission checking.
 *
 * <p>Flow:
 * <ol>
 *   <li>Skip non-API paths (actuator, swagger, etc.)</li>
 *   <li>Skip internal calls (X-Internal-Call header)</li>
 *   <li>Resolve handler method annotations</li>
 *   <li>{@code @PublicApi} -> pass through</li>
 *   <li>{@code @RequirePermission} -> check via query-service</li>
 *   <li>No annotation -> 403 (default-deny)</li>
 * </ol>
 */
public class IamPermissionFilter implements Filter {

    private static final Logger LOG =
            LoggerFactory.getLogger(IamPermissionFilter.class);

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/actuator", "/swagger-ui", "/v3/api-docs",
            "/webjars", "/favicon.ico", "/error");

    private static final String INTERNAL_CALL_HEADER =
            "X-Internal-Call";

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiToPageMapper apiToPageMapper;
    private final IamQueryClient queryClient;
    private final IamSdkProperties properties;

    public IamPermissionFilter(
            RequestMappingHandlerMapping handlerMapping,
            ApiToPageMapper apiToPageMapper,
            IamQueryClient queryClient,
            IamSdkProperties properties) {
        this.handlerMapping = handlerMapping;
        this.apiToPageMapper = apiToPageMapper;
        this.queryClient = queryClient;
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res,
                         FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        try {
            if (shouldSkip(request)) {
                chain.doFilter(req, res);
                return;
            }
            processRequest(request, response, chain);
        } finally {
            IamContext.clear();
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : SKIP_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void processRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        if (isInternalCall(request)) {
            chain.doFilter(request, response);
            return;
        }
        HandlerMethod handler = resolveHandler(request);
        if (handler == null || !isRestController(handler)) {
            chain.doFilter(request, response);
            return;
        }
        dispatchByAnnotation(
                request, response, chain, handler);
    }

    private void dispatchByAnnotation(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            HandlerMethod handler)
            throws IOException, ServletException {
        if (isPublicApi(handler)) {
            chain.doFilter(request, response);
            return;
        }
        RequirePermission perm = findPermission(handler);
        if (perm == null) {
            writeDenied(response,
                    IamResponse403.noPermission());
            return;
        }
        checkAndProceed(
                request, response, chain, perm.value());
    }

    private void checkAndProceed(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            String permissionCode)
            throws IOException, ServletException {
        String userId = request.getHeader("X-User-Id");
        String sysCode = request.getHeader("X-System-Code");
        String orgCode =
                request.getHeader("X-Active-Org-Code");
        if (userId == null || sysCode == null) {
            writeDenied(response,
                    IamResponse403.noPermission());
            return;
        }
        try {
            if (validatePermission(
                    userId, sysCode, orgCode,
                    permissionCode)) {
                chain.doFilter(request, response);
            } else {
                writeDenied(response,
                        IamResponse403.noPermission());
            }
        } catch (Exception e) {
            handleIamError(response, e);
        }
    }

    private boolean validatePermission(
            String userId,
            String systemCode,
            String orgCode,
            String permissionCode) {
        Set<String> pageCodes =
                apiToPageMapper.resolvePageCodes(
                        permissionCode);
        if (pageCodes.isEmpty()) {
            LOG.warn("No page mapping for: {}",
                    permissionCode);
            return false;
        }
        return checkPageSnapshots(
                userId, systemCode, orgCode,
                permissionCode, pageCodes);
    }

    private boolean checkPageSnapshots(
            String userId,
            String systemCode,
            String orgCode,
            String permCode,
            Set<String> pageCodes) {
        // Fetch nav snapshot once per request for lineRoles
        NavSnapshot nav = queryClient.getNavSnapshot(
                systemCode, userId, orgCode);
        List<String> lineRoles = nav != null
                ? nav.lineRoles() : List.of();

        for (String pageCode : pageCodes) {
            PageSnapshot snapshot =
                    queryClient.getPageSnapshot(
                            systemCode, userId,
                            orgCode, pageCode);
            if (snapshot != null
                    && snapshot.apis().contains(permCode)) {
                setContext(userId, systemCode,
                        orgCode, pageCode, snapshot,
                        lineRoles);
                return true;
            }
        }
        return false;
    }

    private void setContext(String userId,
                            String systemCode,
                            String orgCode,
                            String pageCode,
                            PageSnapshot snapshot,
                            List<String> lineRoles) {
        IamContext.set(new IamContext.ContextData(
                userId, systemCode, orgCode,
                pageCode, snapshot, lineRoles));
    }

    private void handleIamError(
            HttpServletResponse response,
            Exception e) throws IOException {
        LOG.error("IAM query failed", e);
        if (properties.getSdk().isFailOpen()) {
            LOG.warn("Fail-open: allowing request");
            return;
        }
        writeDenied(response,
                IamResponse403.iamUnavailable());
    }

    private boolean isInternalCall(
            HttpServletRequest request) {
        return request.getHeader(
                INTERNAL_CALL_HEADER) != null;
    }

    private HandlerMethod resolveHandler(
            HttpServletRequest request) {
        try {
            HandlerExecutionChain chain =
                    handlerMapping.getHandler(request);
            if (chain != null
                    && chain.getHandler()
                    instanceof HandlerMethod hm) {
                return hm;
            }
        } catch (Exception e) {
            LOG.debug("Cannot resolve handler: {}",
                    request.getRequestURI());
        }
        return null;
    }

    private boolean isRestController(HandlerMethod hm) {
        return AnnotatedElementUtils.hasAnnotation(
                hm.getBeanType(), RestController.class);
    }

    private boolean isPublicApi(HandlerMethod hm) {
        return AnnotatedElementUtils.findMergedAnnotation(
                hm.getMethod(), PublicApi.class) != null;
    }

    private RequirePermission findPermission(
            HandlerMethod hm) {
        return AnnotatedElementUtils.findMergedAnnotation(
                hm.getMethod(), RequirePermission.class);
    }

    private void writeDenied(
            HttpServletResponse response,
            IamResponse403 body) throws IOException {
        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(
                "application/json;charset=UTF-8");
        response.getWriter().write(
                MAPPER.writeValueAsString(body));
    }
}
