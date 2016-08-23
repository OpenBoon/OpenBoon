package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchySpec {

    @NotNull
    private Integer folderId;

    @NotEmpty
    private List<DyHierarchyLevel> levels;

    public List<DyHierarchyLevel> getLevels() {
        return levels;
    }

    public DyHierarchySpec setLevels(List<DyHierarchyLevel> levels) {
        this.levels = levels;
        return this;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public DyHierarchySpec setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }
}
