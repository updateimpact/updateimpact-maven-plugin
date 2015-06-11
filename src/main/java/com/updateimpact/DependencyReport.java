package com.updateimpact;

import java.util.Collection;

public class DependencyReport {
    private final String projectName;
    private final String apikey;
    private final String buildId;
    private final Collection<ModuleDependencies> modules;
    private final String formatVersion;

    public DependencyReport(String projectName,
                            String apikey,
                            String buildId,
                            Collection<ModuleDependencies> modules,
                            String formatVersion) {
        this.projectName = projectName;
        this.apikey = apikey;
        this.buildId = buildId;
        this.modules = modules;
        this.formatVersion = formatVersion;
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

    public String getFormatVersion() {
        return formatVersion;
    }
}
