package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchy {

    /**
     * Id of the aggregation.
     */
    private int id;

    /**
     * The folder the agg is on.
     */
    @NotNull
    private Integer folderId;

    /**
     * The user that created the aggregation.
     */
    private String userCreated;

    /**
     * The time the aggregation was created;
     */
    private long timeCreated;

    @NotEmpty
    private List<DyHierarchyLevel> levels;

    private boolean working;

    public int getId() {
        return id;
    }

    public DyHierarchy setId(int id) {
        this.id = id;
        return this;
    }

    public String getUserCreated() {
        return userCreated;
    }

    public DyHierarchy setUserCreated(String userCreated) {
        this.userCreated = userCreated;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public DyHierarchy setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public int getFolderId() {
        return folderId;
    }

    public DyHierarchy setFolderId(int folderId) {
        this.folderId = folderId;
        return this;
    }

    public List<DyHierarchyLevel> getLevels() {
        return levels;
    }

    public DyHierarchy setLevels(List<DyHierarchyLevel> levels) {
        this.levels = levels;
        return this;
    }

    public boolean isWorking() {
        return working;
    }

    public DyHierarchy setWorking(boolean working) {
        this.working = working;
        return this;
    }

    @JsonIgnore
    public DyHierarchyLevel getLevel(int i) {
        return levels.get(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DyHierarchy that = (DyHierarchy) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("folderId", folderId)
                .toString();
    }
}
