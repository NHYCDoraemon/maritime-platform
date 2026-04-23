package com.maritime.iam.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritime.platform.common.core.result.R;
import com.maritime.platform.common.core.security.HmacSignatureValidator;
import com.maritime.iam.sdk.model.NavSnapshot;
import com.maritime.iam.sdk.model.PageSnapshot;
import com.maritime.iam.sdk.model.ResourceNode;
import com.maritime.iam.sdk.workflow.ValidateScopeRequest;
import com.maritime.iam.sdk.workflow.ValidateScopeResponse;
import com.maritime.iam.sdk.workflow.WorkflowOrgView;
import com.maritime.iam.sdk.workflow.WorkflowPage;
import com.maritime.iam.sdk.workflow.WorkflowScope;
import com.maritime.iam.sdk.workflow.WorkflowUserContextView;
import com.maritime.iam.sdk.workflow.WorkflowUserView;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP client for calling iam-query-service APIs.
 * All requests carry HMAC signature headers.
 */
public class IamQueryClient {

    private static final Logger LOG =
            LoggerFactory.getLogger(IamQueryClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final HmacSignatureGenerator hmacGenerator;
    private final ObjectMapper objectMapper;

    public IamQueryClient(RestTemplate restTemplate,
                          String baseUrl,
                          HmacSignatureGenerator hmacGenerator) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.hmacGenerator = hmacGenerator;
        this.objectMapper = new ObjectMapper();
    }

    public NavSnapshot getNavSnapshot(
            String systemCode,
            String userId,
            String activeOrgCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/permissions/nav")
                .queryParam("systemCode", systemCode)
                .queryParam("userId", userId)
                .queryParam("activeOrgCode", activeOrgCode)
                .toUriString();
        return executeGet(url, new TypeReference<>() { });
    }

    public PageSnapshot getPageSnapshot(
            String systemCode,
            String userId,
            String activeOrgCode,
            String pageCode) {
        String url = buildPageUrl(
                systemCode, userId, activeOrgCode, pageCode);
        return executeGet(url, new TypeReference<>() { });
    }

    public boolean checkPermission(
            String systemCode,
            String userId,
            String activeOrgCode,
            String permissionCode) {
        String url = baseUrl + "/api/iam/permissions/check";
        Map<String, String> body = Map.of(
                "systemCode", systemCode,
                "userId", userId,
                "activeOrgCode", activeOrgCode,
                "permissionCode", permissionCode);
        return Boolean.TRUE.equals(
                executePost(url, body, new TypeReference<>() { }));
    }

    @SuppressWarnings("unchecked")
    public List<ResourceNode> getResourceTree(
            String systemCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/resources/tree")
                .queryParam("systemCode", systemCode)
                .toUriString();
        Object data = executeGet(
                url, new TypeReference<R<Object>>() { });
        if (data instanceof List<?> list) {
            return objectMapper.convertValue(
                    list, new TypeReference<>() { });
        }
        return List.of();
    }

    // ─── Workflow-lookup (process-engine ADR-0011) ───────────────────
    //
    // All endpoints live under /api/iam/workflow/* and require the
    // caller's app identity to hold the `iam:workflow:lookup` permission.
    // Scope expansion runs on the IAM side; clients pass the enum + the
    // initiator-org anchor (if the scope needs one) and the result
    // page. Errors surface as RestClientException — callers decide
    // whether to degrade gracefully (NoOp fallback returns empty).

    public WorkflowPage<WorkflowUserView> findWorkflowUsersByRole(
            String roleCode, WorkflowScope scope, String initiatorOrg,
            String explicitOrgCode, String keyword,
            int pageNo, int pageSize) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/workflow/users/by-role")
                .queryParam("roleCode", roleCode)
                .queryParam("scope", scope.name())
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize);
        if (initiatorOrg != null) b.queryParam("initiatorOrg", initiatorOrg);
        if (explicitOrgCode != null) b.queryParam("explicitOrgCode", explicitOrgCode);
        if (keyword != null) b.queryParam("keyword", keyword);
        WorkflowPage<WorkflowUserView> page = executeGet(
                b.toUriString(),
                new TypeReference<R<WorkflowPage<WorkflowUserView>>>() { });
        return page != null ? page : WorkflowPage.empty(pageNo, pageSize);
    }

    public WorkflowPage<WorkflowUserView> findWorkflowUsersByPosition(
            String positionCode, WorkflowScope scope, String initiatorOrg,
            String explicitOrgCode, String keyword,
            int pageNo, int pageSize) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/workflow/users/by-position")
                .queryParam("positionCode", positionCode)
                .queryParam("scope", scope.name())
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize);
        if (initiatorOrg != null) b.queryParam("initiatorOrg", initiatorOrg);
        if (explicitOrgCode != null) b.queryParam("explicitOrgCode", explicitOrgCode);
        if (keyword != null) b.queryParam("keyword", keyword);
        WorkflowPage<WorkflowUserView> page = executeGet(
                b.toUriString(),
                new TypeReference<R<WorkflowPage<WorkflowUserView>>>() { });
        return page != null ? page : WorkflowPage.empty(pageNo, pageSize);
    }

    public WorkflowPage<WorkflowUserView> findWorkflowUsersByOrg(
            String orgCode, WorkflowScope scope, String keyword,
            int pageNo, int pageSize) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/workflow/users/by-org")
                .queryParam("orgCode", orgCode)
                .queryParam("scope", scope.name())
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize);
        if (keyword != null) b.queryParam("keyword", keyword);
        WorkflowPage<WorkflowUserView> page = executeGet(
                b.toUriString(),
                new TypeReference<R<WorkflowPage<WorkflowUserView>>>() { });
        return page != null ? page : WorkflowPage.empty(pageNo, pageSize);
    }

    public WorkflowUserContextView getWorkflowUserContext(String userId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/workflow/users/"
                        + userId + "/context")
                .toUriString();
        return executeGet(url,
                new TypeReference<R<WorkflowUserContextView>>() { });
    }

    public WorkflowPage<WorkflowOrgView> findRoutableOrgs(
            String initiatorOrg, int pageNo, int pageSize) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/workflow/orgs/routable")
                .queryParam("initiatorOrg", initiatorOrg)
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize)
                .toUriString();
        WorkflowPage<WorkflowOrgView> page = executeGet(url,
                new TypeReference<R<WorkflowPage<WorkflowOrgView>>>() { });
        return page != null ? page : WorkflowPage.empty(pageNo, pageSize);
    }

    public ValidateScopeResponse validateWorkflowScope(ValidateScopeRequest request) {
        String url = baseUrl + "/api/iam/workflow/users/validate-scope";
        ValidateScopeResponse resp = executePost(url, request,
                new TypeReference<R<ValidateScopeResponse>>() { });
        if (resp == null) {
            return new ValidateScopeResponse(false, "IAM_UNREACHABLE",
                    "no response from iam-query-service");
        }
        return resp;
    }

    private <T> T executeGet(String url,
                             TypeReference<R<T>> typeRef) {
        HttpHeaders headers = buildHmacHeaders("");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
        return extractData(response.getBody(), typeRef);
    }

    private <T> T executePost(String url,
                              Object body,
                              TypeReference<R<T>> typeRef) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpHeaders headers = buildHmacHeaders(json);
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity =
                    new HttpEntity<>(json, headers);
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url, HttpMethod.POST,
                            entity, String.class);
            return extractData(response.getBody(), typeRef);
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to call IAM API: " + url, e);
        }
    }

    private <T> T extractData(String json,
                              TypeReference<R<T>> typeRef) {
        try {
            R<T> result = objectMapper.readValue(json, typeRef);
            if (result != null && result.isSuccess()) {
                return result.getData();
            }
            LOG.warn("IAM API returned error: {}", json);
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse IAM response", e);
        }
    }

    private HttpHeaders buildHmacHeaders(String body) {
        HmacSignatureGenerator.HmacHeaders hmac =
                hmacGenerator.generate(body);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HmacSignatureValidator.HEADER_APP_CODE,
                hmac.appCode());
        headers.set(HmacSignatureValidator.HEADER_TIMESTAMP,
                hmac.timestamp());
        headers.set(HmacSignatureValidator.HEADER_NONCE,
                hmac.nonce());
        headers.set(HmacSignatureValidator.HEADER_BODY_DIGEST,
                hmac.bodyDigest());
        headers.set(HmacSignatureValidator.HEADER_SIGNATURE,
                hmac.signature());
        return headers;
    }

    private String buildPageUrl(String sys,
                                String uid,
                                String org,
                                String page) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/iam/permissions/page")
                .queryParam("systemCode", sys)
                .queryParam("userId", uid)
                .queryParam("activeOrgCode", org)
                .queryParam("pageCode", page)
                .toUriString();
    }
}
