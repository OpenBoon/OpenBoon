package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by chambers on 6/17/17.
 */
public class Taxonomy {

    private UUID taxonomyId;
    private UUID folderId;
    private boolean active;
    private long timeStarted;
    private long timeStopped;

    public UUID getTaxonomyId() {
        return taxonomyId;
    }

    public Taxonomy setTaxonomyId(UUID taxonomyId) {
        this.taxonomyId = taxonomyId;
        return this;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public Taxonomy setFolderId(UUID folderId) {
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

    /**
     * Used for locking the taxon during processing.
     *
     * @return
     */
    @JsonIgnore
    public String clusterLockId() {
        return "taxi-" + taxonomyId.toString();
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
        return Objects.equals(getTaxonomyId(), taxonomy.getTaxonomyId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaxonomyId());
    }
}
