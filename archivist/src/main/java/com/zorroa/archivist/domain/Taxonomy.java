package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by chambers on 6/17/17.
 */
public class Taxonomy {

    private int taxonomyId;
    private int folderId;
    private boolean active;
    private long timeStarted;
    private long timeStopped;

    public int getTaxonomyId() {
        return taxonomyId;
    }

    public Taxonomy setTaxonomyId(int taxonomyId) {
        this.taxonomyId = taxonomyId;
        return this;
    }

    public int getFolderId() {
        return folderId;
    }

    public Taxonomy setFolderId(int folderId) {
        this.folderId = folderId;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public Taxonomy setActive(boolean active) {
        this.active = active;
        return this;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public Taxonomy setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
        return this;
    }

    public long getTimeStopped() {
        return timeStopped;
    }

    public Taxonomy setTimeStopped(long timeStopped) {
        this.timeStopped = timeStopped;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("taxonomyId", taxonomyId)
                .add("folderId", folderId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Taxonomy taxonomy = (Taxonomy) o;
        return getTaxonomyId() == taxonomy.getTaxonomyId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaxonomyId());
    }
}
