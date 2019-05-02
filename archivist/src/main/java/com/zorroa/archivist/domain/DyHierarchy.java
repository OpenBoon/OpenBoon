package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchy implements Loggable<UUID> {

    /**
     * Id of the aggregation.
     */
    private UUID id;

    /**
     * The folder the agg is on.
     */
    @NotNull
    private UUID folderId;

    /**
     * The user that created the dyhi.
     */
    private UserBase user;

    /**
     * The time the aggregation was created;
     */
    private long timeCreated;

    @NotEmpty
    private List<DyHierarchyLevel> levels;

    private boolean working;

    public UUID getId() {
        return id;
    }

    public DyHierarchy setId(UUID id) {
        this.id = id;
        return this;
    }

    public UserBase getUser() {
        return user;
    }

    public DyHierarchy setUser(UserBase user) {
        this.user = user;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public DyHierarchy setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public DyHierarchy setFolderId(UUID folderId) {
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
    public String getLockName() {
        return "folder_lock:" + this.folderId;
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
        return Objects.equals(getId(), that.getId());
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

    @Override
    public UUID getTargetId() {
        return id;
    }
}
