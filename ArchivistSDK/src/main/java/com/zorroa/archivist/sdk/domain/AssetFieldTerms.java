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

    public void setField(String field) {
        this.field = field;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }
}
