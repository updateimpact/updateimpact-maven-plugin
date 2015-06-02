package com.updateimpact;

import java.util.Collection;

public class DependencyReport {
    private final String apikey;
    private final String buildId;
    private final Collection<DependencyTree> trees;

    public DependencyReport(String apikey, String buildId, Collection<DependencyTree> trees) {
        this.apikey = apikey;
        this.buildId = buildId;
        this.trees = trees;
    }

    public String getApikey() {
        return apikey;
    }

    public String getBuildId() {
        return buildId;
    }

    public Collection<DependencyTree> getTrees() {
        return trees;
    }
}
