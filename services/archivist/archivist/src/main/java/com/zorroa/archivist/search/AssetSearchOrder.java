/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Asset Search Order", description = "Specify the field and order for search results")
public class AssetSearchOrder {

    @ApiModelProperty("Field to order results by.")
    private String field;

    @ApiModelProperty("If true the results will be in ascending order.")
    private Boolean ascending = true;

    public AssetSearchOrder() {

    }

    public AssetSearchOrder(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public AssetSearchOrder setField(String field) {
        this.field = field;
        return this;
    }

    public Boolean getAscending() {
        return ascending;
    }

    public AssetSearchOrder setAscending(Boolean ascending) {
        this.ascending = ascending;
        return this;
    }
}
