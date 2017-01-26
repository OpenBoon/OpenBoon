package com.zorroa.common.repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystBuilder;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Collections;
import java.util.List;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystDaoImpl  extends AbstractElasticDao implements AnalystDao {

    @Override
    public String getType() {
        return "analyst";
    }

    @Override
    public String getIndex() {
        return "analyst";
    }

    @Override
    public String register(String id, AnalystBuilder builder)  {
        byte[] doc = Json.serialize(builder);
        return elastic.index(client.prepareIndex(getIndex(), getType(), id)
                .setSource(doc)
                .setOpType(IndexRequest.OpType.INDEX));
    }

    @Override
    public void setState(String id, AnalystState state) {
        client.prepareUpdate(getIndex(), getType(), id)
                .setDoc(ImmutableMap.of("state", state.ordinal()))
                .setRefresh(true)
                .setRetryOnConflict(5)
                .get();
    }

    private static final JsonRowMapper<Analyst> MAPPER =
            (id, version, source) -> Json.deserialize(source, Analyst.class).setId(id);

    @Override
    public Analyst get(String id) {
        if (id.startsWith("http")) {
            return elastic.queryForObject(client.prepareSearch(getIndex())
                    .setTypes(getType())
                    .setQuery(QueryBuilders.termQuery("url", id)), MAPPER);
        }
        else {
            return elastic.queryForObject(id, MAPPER);
        }
    }

    @Override
    public long count() {
        return elastic.count(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setQuery(QueryBuilders.matchAllQuery()));
    }

    @Override
    public List<Integer> getRunningTaskIds() {
        SearchResponse sr = client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(0)
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(AggregationBuilders.terms("tasks").field("taskIds"))
                .get();

        List<Integer> result = Lists.newArrayList();

        Terms tasks = sr.getAggregations().get("tasks");
        for (Terms.Bucket entry : tasks.getBuckets()) {
            result.add(((Long)entry.getKey()).intValue());
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public PagedList<Analyst> getAll(Pager page) {
        return elastic.page(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(page.getSize())
                .setFrom(page.getFrom())
                .setQuery(QueryBuilders.matchAllQuery()), page, MAPPER);
    }

    @Override
    public List<Analyst> getUnresponsive(int limit, long duration) {
        QueryBuilder query =
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal()))
                        .must(QueryBuilders.rangeQuery("updatedTime").lt(System.currentTimeMillis() - duration));

        return elastic.query(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(limit)
                .setFrom(0)
                .setQuery(query), MAPPER);
    }

    @Override
    public List<Analyst> getActive(Pager paging) {
        QueryBuilder query =
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal()));

        return elastic.query(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(paging.getSize())
                .setFrom(paging.getFrom())
                .addSort("queueSize", SortOrder.ASC)
                .setQuery(query), MAPPER);
    }

    @Override
    public List<Analyst> getReady(Pager paging) {
        QueryBuilder query =
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal()))
                        .must(QueryBuilders.rangeQuery("remainingCapacity").gt(0));

        return elastic.query(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(paging.getSize())
                .setFrom(paging.getFrom())
                .addSort("queueSize", SortOrder.ASC)
                .setQuery(query), MAPPER);
    }
}
