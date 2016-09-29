package com.zorroa.common.elastic;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.search.Scroll;

import java.util.List;
import java.util.Map;

/**
 * Extends PagedList and adds a place to store aggregations, an elasticsearch
 * specific feature.
 */
public class ElasticPagedList<T> extends PagedList<T> {

    private Map<String, Object> aggregations;
    private Scroll scroll;

    public ElasticPagedList() { }

    public ElasticPagedList(Paging page, List<T> list) {
        super(page, list);
    }

    public Map<String, Object> getAggregations() {
        return aggregations;
    }

    public ElasticPagedList setAggregations(Map<String, Object> aggregations) {
        this.aggregations = (Map<String, Object>) aggregations.get("aggregations");
        return this;
    }

    public ElasticPagedList setScroll(Scroll scroll) {
        this.scroll = scroll;
        return this;
    }

    public Scroll getScroll() {
        return scroll;
    }
}
