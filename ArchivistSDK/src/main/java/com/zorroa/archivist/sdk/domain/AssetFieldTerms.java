/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Filter the specified field to match the terms
 */
public class AssetFieldTerms {
    private String field;
    private List<Object> terms;

    public AssetFieldTerms() { }

    public AssetFieldTerms(String field, Object ... terms) {
        this.field = field;
        this.terms = Arrays.asList(terms);
    }

    public AssetFieldTerms(String field, List<Object> terms) {
        this.field = field;
        this.terms = terms;
    }

    public String getField() {
        return field;
    }

    public AssetFieldTerms setField(String field) {
        this.field = field;
        return this;
    }

    public List<Object> getTerms() {
        return terms;
    }

    public AssetFieldTerms setTerms(List<Object> terms) {
        this.terms = terms;
        return this;
    }
}
