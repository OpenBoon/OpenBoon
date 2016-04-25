/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Core search query
 */
public class AssetSearch {

    /**
     * The keyword search being made.
     */
    private String query;

    /**
     * The fields to include in the result.  An empty or null list means all fields.
     */
    private String[] fields;

    /**
     * Filters that are applied to the keyword search results.
     */
    private AssetFilter filter;

    /**
     * The order of the results returned.
     */
    private List<AssetSearchOrder> order;

    /**
     * The keyword confidence level to search.  A value of 0 means
     * confidence filtering is disabled.  Value must be between 0 and 1.
     */
    private double confidence = 0.0;

    /**
     * The size of the result to return.  If left unset the value will
     * default to a server controlled setting.
     */
    private Integer size;

    /**
     * The offset from which to return results from.  This is used for
     * paging large results.  If left unset the value will default
     * to 0.
     */
    private Integer from;

    /**
     * Set to true to do fuzzy matching on the query.
     */
    private boolean fuzzy = true;

    public AssetSearch() {
        this.filter = new AssetFilter();
    }

    public AssetSearch(String query) {
        this();
        this.query = query;
    }

    public AssetSearch(String query, double confidence) {
        this();
        this.query = query;
        this.confidence = confidence;
    }

    public AssetSearch(AssetFilter filter) {
        this.filter = filter;
    }


    public boolean isQuerySet() {
        return (query != null && query.length() > 0);
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

    public Integer getSize() {
        return size;
    }

    public AssetSearch setSize(Integer size) {
        this.size = size;
        return this;
    }

    public Integer getFrom() {
        return from;
    }

    public AssetSearch setFrom(Integer from) {
        this.from = from;
        return this;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public AssetSearch setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
        return this;
    }

    public String[] getFields() {
        return fields;
    }

    public AssetSearch setFields(String[] fields) {
        this.fields = fields;
        return this;
    }
}
