package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private String query;                       // Eg. "food and dog", or see ES Query String DSL for details

    /**
     * The keyword confidence level to search.  There are currently 5 confidence
     * buckets, with 5 being the most confident and 1 being the least.
     *
     * 0 = disabled (all keywords)
     * 5 = searches only confidence level 5
     * 4 = searched 4 and 5
     * 3 = searches 3,4 and 5
     *
     * You get the idea.
     *
     */
    private int confidence = 0;
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

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

}
