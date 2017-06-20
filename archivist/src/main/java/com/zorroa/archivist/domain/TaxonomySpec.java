package com.zorroa.archivist.domain;

import javax.validation.constraints.NotNull;

/**
 * Created by chambers on 6/17/17.
 */
public class TaxonomySpec {

    @NotNull
    private Integer folderId;

    public TaxonomySpec() {}

    public TaxonomySpec(Folder folder) {
        this.folderId = folder.getId();
    }

    public Integer getFolderId() {
        return folderId;
    }

    public TaxonomySpec setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }


}
