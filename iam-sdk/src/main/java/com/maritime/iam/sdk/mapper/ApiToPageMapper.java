package com.maritime.iam.sdk.mapper;

import com.maritime.iam.sdk.client.IamQueryClient;
import com.maritime.iam.sdk.model.ResourceNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and maintains apiCode to pageCode mapping from
 * the IAM resource tree. Supports 1:N mapping (one API can
 * be shared across multiple pages).
 *
 * <p>Uses ReadWriteLock for concurrent access safety
 * per CONCURRENCY_DESIGN.md.
 */
public class ApiToPageMapper {

    private static final Logger LOG =
            LoggerFactory.getLogger(ApiToPageMapper.class);

    private final IamQueryClient queryClient;
    private final String systemCode;
    private final ReadWriteLock lock =
            new ReentrantReadWriteLock();

    private volatile Map<String, Set<String>> apiToPages =
            Collections.emptyMap();

    public ApiToPageMapper(IamQueryClient queryClient,
                           String systemCode) {
        this.queryClient = queryClient;
        this.systemCode = systemCode;
    }

    public Set<String> resolvePageCodes(String apiCode) {
        lock.readLock().lock();
        try {
            return apiToPages.getOrDefault(
                    apiCode, Collections.emptySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void refresh() {
        try {
            List<ResourceNode> tree =
                    queryClient.getResourceTree(systemCode);
            Map<String, Set<String>> newMap =
                    buildMapping(tree);
            lock.writeLock().lock();
            try {
                apiToPages = newMap;
            } finally {
                lock.writeLock().unlock();
            }
            LOG.info("ApiToPageMapper refreshed: {} mappings",
                    newMap.size());
        } catch (Exception e) {
            LOG.error("Failed to refresh API-to-page mapping",
                    e);
        }
    }

    private Map<String, Set<String>> buildMapping(
            List<ResourceNode> tree) {
        if (tree == null || tree.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> codeToParent = new HashMap<>();
        Map<String, String> codeToType = new HashMap<>();
        for (ResourceNode node : tree) {
            codeToParent.put(
                    node.resourceCode(), node.parentCode());
            codeToType.put(
                    node.resourceCode(), node.resourceType());
        }
        return extractApiMappings(
                tree, codeToParent, codeToType);
    }

    private Map<String, Set<String>> extractApiMappings(
            List<ResourceNode> tree,
            Map<String, String> parents,
            Map<String, String> types) {
        Map<String, Set<String>> result = new HashMap<>();
        for (ResourceNode node : tree) {
            if (!"API".equals(node.resourceType())) {
                continue;
            }
            String pageCode = findParentPage(
                    node.resourceCode(), parents, types);
            if (pageCode != null) {
                result.computeIfAbsent(
                        node.resourceCode(),
                        k -> new HashSet<>()).add(pageCode);
            }
        }
        return result;
    }

    private String findParentPage(
            String code,
            Map<String, String> parents,
            Map<String, String> types) {
        String current = parents.get(code);
        int depth = 0;
        while (current != null && depth < 10) {
            if ("PAGE".equals(types.get(current))) {
                return current;
            }
            current = parents.get(current);
            depth++;
        }
        return null;
    }
}
