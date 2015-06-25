package com.updateimpact;

import java.util.Collection;

public class ModuleDependencies {
    private final DependencyId moduleId;
    private final String config;
    private final Collection<Dependency> dependencies;

    public ModuleDependencies(DependencyId moduleId, String config, Collection<Dependency> dependencies) {
        this.moduleId = moduleId;
        this.config = config;
        this.dependencies = dependencies;
    }

    public DependencyId getModuleId() {
        return moduleId;
    }

    public String getConfig() {
        return config;
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }
}
