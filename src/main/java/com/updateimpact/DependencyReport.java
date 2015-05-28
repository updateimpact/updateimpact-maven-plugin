package com.updateimpact;

import java.util.Collection;

public class DependencyReport {
    private final String buildId;
    private final Collection<Dependency> dependencies;

    public DependencyReport(String buildId, Collection<Dependency> dependencies) {
        this.buildId = buildId;
        this.dependencies = dependencies;
    }

    public String getBuildId() {
        return buildId;
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }
}
