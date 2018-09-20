/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.search;

/**
 * Specify the field and order for search results
 */
public class AssetSearchOrder {
    private String field;
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
