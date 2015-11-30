/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Core search query
 */
public class AssetSearch {
    private String query;                       // Eg. "food and dog", or see ES Query String DSL for details
    private AssetFilter filter;                 // Restrict results to match filter
    private List<AssetSearchOrder> order;       // FIXME: Ignored! Multilevel sort order

    /**
     * The keyword confidence level to search.  A value of 0 means
     * confidence filtering is disabled.  Value must be between 0 and 1.
     */
    private double confidence = 0.0;;

    public AssetSearch() { }

    public AssetSearch(String query) {
        this.query = query;
    }

    public AssetSearch(String query, double confidence) {
        this.query = query;
        this.confidence = confidence;
    }

    public String getQuery() {
        return query;
    }

    public AssetSearch setQuery(String query) {
        this.query = query;
        return this;
    }

    public AssetFilter getFilter() {
        return filter;
    }

    public AssetSearch setFilter(AssetFilter filter) {
        this.filter = filter;
        return this;
    }

    public List<AssetSearchOrder> getOrder() {
        return order;
    }

    public AssetSearch setOrder(List<AssetSearchOrder> order) {
        this.order = order;
        return this;
    }

    public double getConfidence() {
        return confidence;
    }

    public AssetSearch setConfidence(double confidence) {
        this.confidence = confidence;
        return this;
    }
}
