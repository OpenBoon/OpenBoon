package com.zorroa.archivist.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Created by chambers on 6/17/17.
 */
@ApiModel(value = "Taxonomy Spec", description = "Attributes required to create a Taxonomy.")
public class TaxonomySpec {

    @NotNull
    @ApiModelProperty("UUID of the root Folder for the Taxonomy.")
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
