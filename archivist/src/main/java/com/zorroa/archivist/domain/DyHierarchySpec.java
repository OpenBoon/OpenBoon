package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchySpec {

    @NotNull
    private UUID folderId;

    @NotEmpty
    private List<DyHierarchyLevel> levels;

    public List<DyHierarchyLevel> getLevels() {
        return levels;
    }

    public DyHierarchySpec setLevels(List<DyHierarchyLevel> levels) {
        this.levels = levels;
        return this;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public DyHierarchySpec setFolderId(UUID folderId) {
        this.folderId = folderId;
        return this;
    }
}
