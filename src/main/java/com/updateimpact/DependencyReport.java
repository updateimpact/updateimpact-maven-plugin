package com.updateimpact;

import java.util.Collection;

public class DependencyReport {
    private final String projectName;
    private final String apikey;
    private final String buildId;
    private final Collection<ModuleDependencies> modules;
    private final String version;

    public DependencyReport(String projectName,
                            String apikey,
                            String buildId,
                            Collection<ModuleDependencies> modules,
                            String version) {
        this.projectName = projectName;
        this.apikey = apikey;
        this.buildId = buildId;
        this.modules = modules;
        this.version = version;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getApikey() {
        return apikey;
    }

    public String getBuildId() {
        return buildId;
    }

    public Collection<ModuleDependencies> getModules() {
        return modules;
    }

    public String getVersion() {
        return version;
    }
}
