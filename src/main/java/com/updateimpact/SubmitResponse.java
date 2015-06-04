package com.updateimpact;

public class SubmitResponse {
    private final String userId;
    private final String buildId;

    public SubmitResponse(String userId, String buildId) {
        this.userId = userId;
        this.buildId = buildId;
    }

    public String getUserId() {
        return userId;
    }

    public String getBuildId() {
        return buildId;
    }
}
