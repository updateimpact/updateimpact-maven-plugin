package com.updateimpact;

import org.apache.maven.shared.dependency.tree.DependencyNode;

public class DependencyChild {
    private final DependencyId id;
    private final String evictedByVersion;
    private final Boolean cycle;

    public DependencyChild(DependencyId id, String evictedByVersion, Boolean cycle) {
        this.id = id;
        this.evictedByVersion = evictedByVersion;
        this.cycle = cycle;
    }

    public DependencyId getId() {
        return id;
    }

    public String getEvictedByVersion() {
        return evictedByVersion;
    }

    public Boolean getCycle() {
        return cycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyChild that = (DependencyChild) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public static DependencyChild fromNode(DependencyNode node) {
        return new DependencyChild(
                DependencyId.fromNode(node),
                node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact().getVersion() : null,
                node.getState() == DependencyNode.OMITTED_FOR_CYCLE ? true : null
        );
    }
}
