package com.zorroa.common.elastic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.common.domain.Paging;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps an elastic result into a simpler json serializable object.  Includes
 * the search result and aggregations.
 */
public class PagedElasticList implements Iterable<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(PagedElasticList.class);

    private List<Map<String, Object>> list;

    private Map<String, Map<String, Object>> aggregations;

    private final Paging page;

    public PagedElasticList(SearchResponse rsp, Paging page) {
        this.list = Lists.newArrayListWithCapacity(Math.toIntExact(rsp.getHits().getTotalHits()));
        for (SearchHit hit: rsp.getHits().getHits()) {
            this.list.add(hit.getSource());
        }
        this.page = page;
        this.page.setTotalCount(rsp.getHits().getTotalHits());

        if (rsp.getAggregations() != null) {
            List<Aggregation> aggs = rsp.getAggregations().asList();
            this.aggregations = Maps.newHashMapWithExpectedSize(aggs.size());
            for (Aggregation agg : aggs) {
                Terms t = (Terms) agg;
                Map<String, Object> buckets = Maps.newHashMap();
                for (Terms.Bucket b : t.getBuckets()) {
                    buckets.put(b.getKey().toString(), b.getDocCount());
                }
                this.aggregations.put(agg.getName(), buckets);
            }
        }
        else {
            this.aggregations = ImmutableMap.of();
        }
    }

    public List<Map<String, Object>> getList() {
        return list;
    }

    public Map<String, Map<String, Object>> getAggregations() {
        return aggregations;
    }

    public Paging getPage() {
        return page;
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return this.list.iterator();
    }
}
