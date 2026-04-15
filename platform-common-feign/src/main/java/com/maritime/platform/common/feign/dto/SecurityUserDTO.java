package com.maritime.platform.common.feign.dto;

import java.io.Serializable;
import java.util.List;

public class SecurityUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String userName;
    private String activeOrgCode;
    private List<String> systemScope;
    private String sessionId;

    public SecurityUserDTO() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getActiveOrgCode() {
        return activeOrgCode;
    }

    public void setActiveOrgCode(String activeOrgCode) {
        this.activeOrgCode = activeOrgCode;
    }

    public List<String> getSystemScope() {
        return systemScope;
    }

    public void setSystemScope(List<String> systemScope) {
        this.systemScope = systemScope;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
