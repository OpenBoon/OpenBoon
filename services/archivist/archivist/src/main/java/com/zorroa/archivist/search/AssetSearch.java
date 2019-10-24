/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiModel(value = "Asset Search", description = "Describes all possible properties which can be used when searching for assets.")
public class AssetSearch {


    @ApiModelProperty("Create a scrolling search.  Set this to the value of the timeout.")
    private Scroll scroll;

    @ApiModelProperty("The keyword search being made.")
    private String query;

    @ApiModelProperty("Search for exactQuery terms as if each word was quoted.")
    private Boolean exactQuery = false;

    @ApiModelProperty("Map of query fields and boost values.")
    private Map<String, Float> queryFields;

    @ApiModelProperty("Fields to include in the result.  An empty or null list means all fields.")
    private String[] fields;

    @ApiModelProperty("Filters that are applied to the keyword search results and aggs.")
    private AssetFilter filter;

    @ApiModelProperty("Post-filters are applied to search results after processing aggs.")
    private AssetFilter postFilter;

    @ApiModelProperty("Sets the order of the results returned.")
    private List<AssetSearchOrder> order;

    @ApiModelProperty("Size of the result to return. If left unset the value will default to a server " +
            "controlled setting.")
    private Integer size;

    @ApiModelProperty("Page from which to return results from. This is used for paging large results. If left unset " +
            "the value will default to 1.")
    private Integer from;

    @ApiModelProperty("Allow for field collapsing. https://www.elastic.co/guide/en/elasticsearch/reference/" +
            "6.4/search-request-collapse.html")
    private Map<String, Object> collapse;

    @ApiModelProperty("Aggregations to return with the search.  Map is name, then agg.")
    private Map<String, Map<String, Object>> aggs;

    public AssetSearch() {
    }

    public AssetSearch(String query) {
        this.query = query;
    }

    public AssetSearch(AssetFilter filter) {
        this.filter = filter;
    }

    @JsonIgnore
    public boolean isQuerySet() {
        return (query != null && !query.isEmpty());
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

    public AssetFilter getPostFilter() {
        return postFilter;
    }

    public void setPostFilter(AssetFilter postFilter) {
        this.postFilter = postFilter;
    }

    public AssetFilter addToFilter() {
        if (filter == null) {
            filter = new AssetFilter();
        }
        return filter;
    }

    public List<AssetSearchOrder> getOrder() {
        return order;
    }

    public AssetSearch setOrder(List<AssetSearchOrder> order) {
        this.order = order;
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

    public void setFrom(Integer from) {
        this.from = from;
    }

    public AssetSearch setPageAndSize(Integer page, Integer size) {
        if (page != null && size != null) {
            this.from = page > 0 ? (page - 1) * size : 0;
            this.size = size;
        }
        return this;
    }

    @JsonIgnore
    public Integer getClosestPage() {
        if (this.from != null && this.size != null) {
            return this.from / this.size + 1;
        }
        return 1;
    }

    public String[] getFields() {
        return fields;
    }

    public AssetSearch setFields(String[] fields) {
        this.fields = fields;
        return this;
    }

    public Scroll getScroll() {
        return scroll;
    }

    public AssetSearch setScroll(Scroll scroll) {
        this.scroll = scroll;
        return this;
    }

    public Map<String, Float> getQueryFields() {
        return queryFields;
    }

    public AssetSearch setQueryFields(Map<String, Float> queryFields) {
        this.queryFields = queryFields;
        return this;
    }

    public Map<String, Map<String, Object>> getAggs() {
        return aggs;
    }

    public AssetSearch setAggs(Map<String, Map<String, Object>> aggs) {
        this.aggs = aggs;
        return this;
    }

    public AssetSearch addToAggs(String name, Map<String, Object> agg) {
        if (aggs == null) {
            aggs = new HashMap();
        }
        aggs.put(name, agg);
        return this;
    }

    public Boolean isExactQuery() {
        return exactQuery;
    }

    public AssetSearch setExactQuery(Boolean exactQuery) {
        this.exactQuery = exactQuery;
        return this;
    }

    public Map<String, Object> getCollapse() {
        return collapse;
    }

    public void setCollapse(Map<String, Object> collapse) {
        this.collapse = collapse;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (query == null || query.isEmpty()) && (filter == null || filter.isEmpty());
    }
}
