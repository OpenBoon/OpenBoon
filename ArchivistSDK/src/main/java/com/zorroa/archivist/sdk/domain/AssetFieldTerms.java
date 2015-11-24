/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Filter the specified field to match the terms
 */
public class AssetFieldTerms {
    private String field;
    private List<String> terms;

    public String getField() {
        return field;
    }

    public AssetFieldTerms setField(String field) {
        this.field = field;
        return this;
    }

    public List<String> getTerms() {
        return terms;
    }

    public AssetFieldTerms setTerms(List<String> terms) {
        this.terms = terms;
        return this;
    }
}
