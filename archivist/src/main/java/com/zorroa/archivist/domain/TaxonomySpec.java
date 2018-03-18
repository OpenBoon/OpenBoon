package com.zorroa.archivist.domain;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Created by chambers on 6/17/17.
 */
public class TaxonomySpec {

    @NotNull
    private UUID folderId;

    public TaxonomySpec() {}

    public TaxonomySpec(Folder folder) {
        this.folderId = folder.getId();
    }

    public UUID getFolderId() {
        return folderId;
    }

    public TaxonomySpec setFolderId(UUID folderId) {
        this.folderId = folderId;
        return this;
    }


}
