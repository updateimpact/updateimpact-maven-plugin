package com.updateimpact;

import java.util.Collection;

public class DependencyReport {
    private final String apikey;
    private final String buildId;
    private final Collection<Dependency> dependencies;

    public DependencyReport(String apikey, String buildId, Collection<Dependency> dependencies) {
        this.apikey = apikey;
        this.buildId = buildId;
        this.dependencies = dependencies;
    }

    public String getApikey() {
        return apikey;
    }

    public String getBuildId() {
        return buildId;
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }
}
