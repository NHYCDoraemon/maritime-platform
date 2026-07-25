package com.maritime.iam.sdk.permission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive IAM permission client for Spring Cloud Gateway consumers.
 *
 * <p>The caller supplies identity previously verified by its trusted
 * gateway. IAM remains the source of truth for effective permission
 * calculation.</p>
 */
public class ReactiveIamPermissionClient {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ACTIVE_ORG_HEADER =
            "X-Active-Org-Code";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ReactiveIamPermissionClient(WebClient webClient) {
        this(webClient, new ObjectMapper());
    }

    ReactiveIamPermissionClient(
            WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Query effective resource and role codes without IAM-side caching.
     */
    public Mono<String> getPermissionCodes(
            String systemCode, String userId,
            String activeOrgCode) {
        return execute(systemCode, userId, activeOrgCode,
                "/api/iam/permissions/codes")
                .map(this::readTextData);
    }

    /**
     * Query the monotonic permission version maintained by IAM.
     */
    public Mono<String> getPermissionVersion(
            String systemCode, String userId,
            String activeOrgCode) {
        return execute(systemCode, userId, activeOrgCode,
                "/api/iam/permissions/version")
                .map(this::readVersionData);
    }

    private Mono<String> execute(
            String systemCode, String userId,
            String activeOrgCode, String path) {
        return webClient.get()
                .uri(builder -> builder.path(path)
                        .queryParam("systemCode", systemCode)
                        .build())
                .headers(headers -> addIdentityHeaders(
                        headers, userId, activeOrgCode))
                .exchangeToMono(response -> {
                    if (response.statusCode()
                            .equals(HttpStatus.NOT_FOUND)) {
                        return Mono.error(
                                new PermissionVersionEndpointUnsupportedException());
                    }
                    if (response.statusCode().isError()) {
                        return response.createException()
                                .flatMap(Mono::error);
                    }
                    return response.bodyToMono(String.class);
                });
    }

    private void addIdentityHeaders(
            HttpHeaders headers, String userId,
            String activeOrgCode) {
        headers.set(USER_ID_HEADER, userId);
        if (activeOrgCode != null
                && !activeOrgCode.isBlank()) {
            headers.set(ACTIVE_ORG_HEADER, activeOrgCode);
        }
    }

    private String readTextData(String body) {
        JsonNode data = readData(body);
        return data == null || data.isNull()
                ? "" : data.asText("");
    }

    private String readVersionData(String body) {
        JsonNode data = readData(body);
        if (data == null || data.isNull()
                || data.asText("").isBlank()) {
            throw new IamPermissionUnavailableException(
                    "IAM returned an invalid permission version");
        }
        return data.asText();
    }

    private JsonNode readData(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null
                    || !root.path("success").asBoolean(false)) {
                throw new IamPermissionUnavailableException(
                        "IAM permission query was not successful");
            }
            return root.get("data");
        } catch (IamPermissionUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new IamPermissionUnavailableException(
                    "Cannot parse IAM permission response", e);
        }
    }
}
