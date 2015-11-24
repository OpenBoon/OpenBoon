package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private String query;                       // Eg. "food and dog", or see ES Query String DSL for details
    private AssetFilter filter;                 // Restrict results to match filter
    private List<AssetSearchOrder> order;       // FIXME: Ignored! Multilevel sort order

    public AssetSearchBuilder() { }

    public String getQuery() {
        return query;
    }

    public AssetSearchBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    public AssetFilter getFilter() {
        return filter;
    }

    public AssetSearchBuilder setFilter(AssetFilter filter) {
        this.filter = filter;
        return this;
    }

    public List<AssetSearchOrder> getOrder() {
        return order;
    }

    public AssetSearchBuilder setOrder(List<AssetSearchOrder> order) {
        this.order = order;
        return this;
    }
}
