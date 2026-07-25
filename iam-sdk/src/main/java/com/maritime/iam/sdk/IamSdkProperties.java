package com.maritime.iam.sdk;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for iam-sdk.
 *
 * <pre>
 * iam:
 *   center:
 *     url: http://iam-query-service:9083
 *   app:
 *     code: HSJG
 *     secret: ${IAM_APP_SECRET}
 *   sdk:
 *     fail-open: false
 *     cache:
 *       permission-codes-ttl: 30m
 *       empty-permission-codes-ttl: 2m
 *       version-check-interval: 1s
 *       max-permission-header-bytes: 16384
 *     scope:
 *       org-column: org_id        # default — override to e.g. creator_org_code
 *       self-column: user_id      # default — override to e.g. creator_user_id
 *       line-type-column: line_type
 *   event:
 *     enabled: false
 * </pre>
 */
@ConfigurationProperties(prefix = "iam")
public class IamSdkProperties {

    private Center center = new Center();
    private App app = new App();
    private Sdk sdk = new Sdk();
    private Event event = new Event();

    public Center getCenter() {
        return center;
    }

    public void setCenter(Center center) {
        this.center = center;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public Sdk getSdk() {
        return sdk;
    }

    public void setSdk(Sdk sdk) {
        this.sdk = sdk;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public static class Center {

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class App {

        private String code;
        private String secret;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Sdk {

        private boolean failOpen = false;
        private Scope scope = new Scope();
        private Cache cache = new Cache();

        public boolean isFailOpen() {
            return failOpen;
        }

        public void setFailOpen(boolean failOpen) {
            this.failOpen = failOpen;
        }

        public Scope getScope() {
            return scope;
        }

        public void setScope(Scope scope) {
            this.scope = scope;
        }

        public Cache getCache() {
            return cache;
        }

        public void setCache(Cache cache) {
            this.cache = cache;
        }
    }

    /**
     * Business-side permission cache and reconciliation settings.
     */
    public static class Cache {

        private Duration permissionCodesTtl =
                Duration.ofMinutes(30);
        private Duration emptyPermissionCodesTtl =
                Duration.ofMinutes(2);
        private Duration versionCheckInterval =
                Duration.ofSeconds(1);
        private int maxPermissionHeaderBytes = 16 * 1024;

        public Duration getPermissionCodesTtl() {
            return permissionCodesTtl;
        }

        public void setPermissionCodesTtl(
                Duration permissionCodesTtl) {
            this.permissionCodesTtl = permissionCodesTtl;
        }

        public Duration getEmptyPermissionCodesTtl() {
            return emptyPermissionCodesTtl;
        }

        public void setEmptyPermissionCodesTtl(
                Duration emptyPermissionCodesTtl) {
            this.emptyPermissionCodesTtl =
                    emptyPermissionCodesTtl;
        }

        public Duration getVersionCheckInterval() {
            return versionCheckInterval;
        }

        public void setVersionCheckInterval(
                Duration versionCheckInterval) {
            this.versionCheckInterval = versionCheckInterval;
        }

        public int getMaxPermissionHeaderBytes() {
            return maxPermissionHeaderBytes;
        }

        public void setMaxPermissionHeaderBytes(
                int maxPermissionHeaderBytes) {
            this.maxPermissionHeaderBytes =
                    maxPermissionHeaderBytes;
        }
    }

    /**
     * Column-name overrides for data-permission injection.
     * Defaults preserve pre-1.0.8 hard-coded behaviour
     * ({@code org_id} / {@code user_id} / {@code line_type}).
     */
    public static class Scope {

        private String orgColumn = "org_id";
        private String selfColumn = "user_id";
        private String lineTypeColumn = "line_type";

        public String getOrgColumn() {
            return orgColumn;
        }

        public void setOrgColumn(String orgColumn) {
            this.orgColumn = orgColumn;
        }

        public String getSelfColumn() {
            return selfColumn;
        }

        public void setSelfColumn(String selfColumn) {
            this.selfColumn = selfColumn;
        }

        public String getLineTypeColumn() {
            return lineTypeColumn;
        }

        public void setLineTypeColumn(String lineTypeColumn) {
            this.lineTypeColumn = lineTypeColumn;
        }
    }

    public static class Event {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
