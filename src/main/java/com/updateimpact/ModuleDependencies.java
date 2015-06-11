package com.updateimpact;

import java.util.Collection;

public class ModuleDependencies {
    private final DependencyId moduleId;
    private final Collection<Dependency> dependencies;

    public ModuleDependencies(DependencyId moduleId, Collection<Dependency> dependencies) {
        this.moduleId = moduleId;
        this.dependencies = dependencies;
    }

    public DependencyId getModuleId() {
        return moduleId;
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }
}
