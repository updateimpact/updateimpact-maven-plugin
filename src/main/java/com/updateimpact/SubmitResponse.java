package com.updateimpact;

public class SubmitResponse {
    private final String userIdStr;
    private final String buildId;

    public SubmitResponse(String userIdStr, String buildId) {
        this.userIdStr = userIdStr;
        this.buildId = buildId;
    }

    public String getUserIdStr() {
        return userIdStr;
    }

    public String getBuildId() {
        return buildId;
    }
}
