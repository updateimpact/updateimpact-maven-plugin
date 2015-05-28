package com.updateimpact;

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
}
