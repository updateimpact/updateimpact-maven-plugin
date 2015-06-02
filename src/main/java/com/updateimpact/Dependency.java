package com.updateimpact;

import java.util.List;

public class Dependency {
    private final DependencyId id;
    private final List<DependencyChild> dependencies;

    public Dependency(DependencyId id, List<DependencyChild> dependencies) {
        this.id = id;
        this.dependencies = dependencies;
    }

    public DependencyId getId() {
        return id;
    }

    public List<DependencyChild> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dependency that = (Dependency) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
