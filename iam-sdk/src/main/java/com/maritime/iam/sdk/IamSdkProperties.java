package com.maritime.iam.sdk;

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

        public boolean isFailOpen() {
            return failOpen;
        }

        public void setFailOpen(boolean failOpen) {
            this.failOpen = failOpen;
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
