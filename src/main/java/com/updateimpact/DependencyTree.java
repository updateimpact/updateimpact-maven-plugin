package com.updateimpact;

import java.util.Collection;

public class DependencyTree {
    private final DependencyId rootId;
    private final Collection<Dependency> dependencies;

    public DependencyTree(DependencyId rootId, Collection<Dependency> dependencies) {
        this.rootId = rootId;
        this.dependencies = dependencies;
    }

    public DependencyId getRootId() {
        return rootId;
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }
}
