/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

/**
 * Filter for field values in the specified range
 */
public class AssetFieldRange {
    private String field;
    private String min;
    private String max;

    public String getField() {
        return field;
    }

    public AssetFieldRange setField(String field) {
        this.field = field;
        return this;
    }

    public String getMin() {
        return min;
    }

    public AssetFieldRange setMin(String min) {
        this.min = min;
        return this;
    }

    public String getMax() {
        return max;
    }

    public AssetFieldRange setMax(String max) {
        this.max = max;
        return this;
    }
}
