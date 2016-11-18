package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by chambers on 11/18/16.
 */
public class AssetPermissionUpdate {

    @NotEmpty
    private String type;

    @NotNull
    private Integer id;

    @NotEmpty
    private List<String> assetIds;

    public String getType() {
        return type;
    }

    public AssetPermissionUpdate setType(String type) {
        this.type = type;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AssetPermissionUpdate setId(Integer id) {
        this.id = id;
        return this;
    }

    public List<String> getAssetIds() {
        return assetIds;
    }

    public AssetPermissionUpdate setAssetIds(List<String> assetIds) {
        this.assetIds = assetIds;
        return this;
    }
}
