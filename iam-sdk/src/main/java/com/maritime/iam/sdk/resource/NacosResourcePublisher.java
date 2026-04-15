package com.maritime.iam.sdk.resource;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * On startup, loads iam-resources.yml, flattens the tree,
 * and publishes to Nacos config center.
 */
public class NacosResourcePublisher {

    private static final Logger LOG =
            LoggerFactory.getLogger(NacosResourcePublisher.class);

    private static final String GROUP = "IAM_RESOURCE_GROUP";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${iam.resource.nacos-addr:${NACOS_SERVER_ADDR:localhost:8848}}")
    private String nacosAddr;

    @Value("${iam.resource.nacos-namespace:}")
    private String nacosNamespace;

    private ConfigService configService;

    @PostConstruct
    public void init() {
        try {
            Properties props = new Properties();
            props.setProperty("serverAddr", nacosAddr);
            if (nacosNamespace != null
                    && !nacosNamespace.isBlank()) {
                props.setProperty("namespace", nacosNamespace);
            }
            this.configService =
                    NacosFactory.createConfigService(props);
            LOG.info("Nacos ConfigService initialized: addr={}",
                    nacosAddr);
        } catch (Exception e) {
            LOG.warn("Failed to init Nacos ConfigService: {}",
                    e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publish() {
        if (configService == null) {
            LOG.warn("Nacos ConfigService not available, "
                    + "skipping resource registration");
            return;
        }

        ResourceManifest manifest = loadManifest();
        if (manifest == null) {
            LOG.info("No iam-resources.yml found, skipping");
            return;
        }

        String dataId = manifest.getSystemCode() + "."
                + manifest.getModuleCode() + ".iam-resources";
        List<FlatResourceNode> nodes = flatten(manifest);

        try {
            String json = MAPPER.writeValueAsString(nodes);

            // Try gRPC first, fallback to HTTP API
            boolean ok = false;
            try {
                ok = configService.publishConfig(
                        dataId, GROUP, json, "json");
            } catch (Exception grpcErr) {
                LOG.debug("gRPC publish failed, trying HTTP: {}",
                        grpcErr.getMessage());
            }

            if (!ok) {
                // Fallback: Nacos HTTP OpenAPI
                ok = publishViaHttp(dataId, json);
            }

            if (ok) {
                LOG.info("Published {} resources to Nacos: "
                                + "dataId={}, group={}",
                        nodes.size(), dataId, GROUP);
            } else {
                LOG.error("Nacos publishConfig failed: "
                        + "dataId={}", dataId);
            }
        } catch (Exception e) {
            LOG.warn("Failed to publish resources to Nacos "
                    + "(non-fatal): {}", e.getMessage());
        }
    }

    private ResourceManifest loadManifest() {
        try {
            ClassPathResource res =
                    new ClassPathResource("iam-resources.yml");
            if (!res.exists()) {
                return null;
            }
            try (InputStream is = res.getInputStream()) {
                Yaml yaml = new Yaml();
                Map<String, Object> root = yaml.load(is);
                @SuppressWarnings("unchecked")
                Map<String, Object> iamSection =
                        (Map<String, Object>) root.get("iam");
                if (iamSection == null) return null;
                @SuppressWarnings("unchecked")
                Map<String, Object> resourceSection =
                        (Map<String, Object>) iamSection
                                .get("resource");
                if (resourceSection == null) return null;

                ResourceManifest m = new ResourceManifest();
                m.setSystemCode(
                        (String) resourceSection.get("system-code"));
                m.setModuleCode(
                        (String) resourceSection.get("module-code"));
                m.setModuleName(
                        (String) resourceSection.get("module-name"));

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawResources =
                        (List<Map<String, Object>>) resourceSection
                                .get("resources");
                if (rawResources != null) {
                    m.setResources(parseResources(rawResources));
                }
                return m;
            }
        } catch (Exception e) {
            LOG.warn("Failed to load iam-resources.yml: {}",
                    e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceDefinition> parseResources(
            List<Map<String, Object>> rawList) {
        List<ResourceDefinition> result = new ArrayList<>();
        for (Map<String, Object> raw : rawList) {
            ResourceDefinition rd = new ResourceDefinition();
            rd.setCode((String) raw.get("code"));
            rd.setName((String) raw.get("name"));
            rd.setType((String) raw.get("type"));
            rd.setRoutePath((String) raw.get("route-path"));
            rd.setComponent((String) raw.get("component"));
            rd.setPermissionExpr(
                    (String) raw.get("permission-expr"));
            rd.setSortNo(raw.get("sort-no") instanceof Number n
                    ? n.intValue() : null);
            List<Map<String, Object>> children =
                    (List<Map<String, Object>>) raw.get("children");
            if (children != null) {
                rd.setChildren(parseResources(children));
            }
            result.add(rd);
        }
        return result;
    }

    private List<FlatResourceNode> flatten(ResourceManifest m) {
        List<FlatResourceNode> nodes = new ArrayList<>();
        if (m.getResources() == null) return nodes;
        int sortCounter = 0;
        for (ResourceDefinition rd : m.getResources()) {
            sortCounter = flattenNode(
                    rd, m.getModuleCode(), nodes, sortCounter);
        }
        return nodes;
    }

    private int flattenNode(ResourceDefinition node,
                            String parentCode,
                            List<FlatResourceNode> nodes,
                            int sortCounter) {
        nodes.add(new FlatResourceNode(
                node.getCode(),
                node.getName(),
                node.getType(),
                parentCode,
                node.getRoutePath(),
                node.getComponent(),
                node.getPermissionExpr(),
                node.getSortNo() != null
                        ? node.getSortNo() : sortCounter));
        sortCounter++;

        if (node.getChildren() != null) {
            for (ResourceDefinition child : node.getChildren()) {
                sortCounter = flattenNode(
                        child, node.getCode(), nodes, sortCounter);
            }
        }
        return sortCounter;
    }

    /**
     * Fallback: publish via Nacos HTTP OpenAPI (bypasses gRPC).
     */
    private boolean publishViaHttp(String dataId, String content) {
        try {
            String url = "http://" + nacosAddr
                    + "/nacos/v1/cs/configs";
            String params = "dataId="
                    + java.net.URLEncoder.encode(dataId, "UTF-8")
                    + "&group="
                    + java.net.URLEncoder.encode(GROUP, "UTF-8")
                    + "&type=json&content="
                    + java.net.URLEncoder.encode(content, "UTF-8");
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new java.net.URL(url)
                            .openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getOutputStream()
                    .write(params.getBytes("UTF-8"));
            int code = conn.getResponseCode();
            String body = new String(
                    conn.getInputStream().readAllBytes());
            conn.disconnect();
            LOG.info("Nacos HTTP publish: code={}, body={}",
                    code, body);
            return "true".equals(body.trim());
        } catch (Exception e) {
            LOG.error("Nacos HTTP publish failed: {}",
                    e.getMessage());
            return false;
        }
    }
}
