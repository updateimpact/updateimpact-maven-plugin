package com.updateimpact;

import java.util.List;

public class Dependency {
    private final DependencyId id;
    private final Boolean root;
    private final List<DependencyChild> dependencies;

    public Dependency(DependencyId id, Boolean root, List<DependencyChild> dependencies) {
        this.id = id;
        this.root = root;
        this.dependencies = dependencies;
    }

    public DependencyId getId() {
        return id;
    }

    public Boolean getRoot() {
        return root;
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
