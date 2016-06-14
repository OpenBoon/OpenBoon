package com.zorroa.common.elastic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 6/14/16.
 */
public class SerializableElasticResult implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SerializableElasticResult.class);

    /**
     *
     */
    private final List<Map<String, Object>> hits;

    /**
     *
     */
    private final Map<String, Map<String, Object>> aggregations;

    /**
     *
     */
    private final long total;

    public SerializableElasticResult(SearchResponse rsp) {
        this.hits = Lists.newArrayListWithCapacity(Math.toIntExact(rsp.getHits().getTotalHits()));
        for (SearchHit hit: rsp.getHits().getHits()) {
            this.hits.add(hit.getSource());
        }
        this.total = rsp.getHits().totalHits();

        List<Aggregation> aggs = rsp.getAggregations().asList();
        this.aggregations = Maps.newHashMapWithExpectedSize(aggs.size());
        for (Aggregation agg: aggs) {
            Terms t = (Terms) agg;
            Map<String, Object> buckets = Maps.newHashMap();
            for (Terms.Bucket b: t.getBuckets()) {
                buckets.put(b.getKey().toString(), b.getDocCount());
            }
            this.aggregations.put(agg.getName(), buckets);
        }
    }

    public List<Map<String, Object>> getHits() {
        return hits;
    }


    public long getTotal() {
        return total;
    }

    public Map<String, Map<String, Object>> getAggregations() {
        return aggregations;
    }
}
