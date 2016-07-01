package com.zorroa.common.elastic;

import com.google.common.collect.Lists;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;

public class ElasticTemplate {

    private Client client;
    private String index;
    private String type;

    public ElasticTemplate(Client client, String index, String type) {
        this.client = client;
        this.index = index;
        this.type = type;
    }

    public <T> T queryForObject(String id, JsonRowMapper<T> mapper) {
        final GetRequestBuilder builder = client.prepareGet(index, type, id).setFetchSource(true);
        final GetResponse r = builder.get();
        if (!r.isExists()) {
            throw new EmptyResultDataAccessException(
                    "Expected 1 '" + type + "' of id '" + id + "'", 0);
        }
        try {
            return mapper.mapRow(r.getId(), r.getVersion(), r.getSourceAsBytes());
        } catch (Exception e) {
            throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
        }
    }

    public <T> T queryForObject(SearchRequestBuilder builder, JsonRowMapper<T> mapper) {
        final SearchResponse r = builder.get();
        if (r.getHits().getTotalHits() == 0) {
            throw new EmptyResultDataAccessException("Expected 1, was", 0);
        }
        SearchHit hit = r.getHits().getAt(0);
        try {
            return mapper.mapRow(hit.getId(), hit.getVersion(), hit.source());
        } catch (Exception e) {
            throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
        }
    }

    public <T> List<T> query(JsonRowMapper<T> mapper) {
        final SearchResponse r = client.prepareSearch(index)
            .setQuery(QueryBuilders.matchAllQuery())
            .setTypes(type)
            .setIndices(index)
            .get();

        List<T> result = Lists.newArrayListWithCapacity((int)r.getHits().getTotalHits());
        for (SearchHit hit: r.getHits()) {
            try {
                result.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }
        return result;
    }

    public <T> List<T> query(SearchRequestBuilder builder, JsonRowMapper<T> mapper) {
        final SearchResponse r = builder.get();
        List<T> result = Lists.newArrayListWithCapacity((int)r.getHits().getTotalHits());

        for (SearchHit hit: r.getHits()) {
            try {
                result.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }
        return result;
    }

    public <T> PagedList<List<T>> page(SearchRequestBuilder builder, Paging paging, JsonRowMapper<T> mapper) {
        builder.setSize(paging.getCount()).setFrom(paging.getFrom());
        final SearchResponse r = builder.get();
        List<T> result = Lists.newArrayListWithCapacity((int)r.getHits().getTotalHits());

        for (SearchHit hit: r.getHits()) {
            try {
                result.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }
        paging.setTotalCount(r.getHits().getTotalHits());
        return new PagedList(paging, result);
    }

    /**
     * Return the count for the given search.
     *
     * @param builder
     * @return
     */
    public long count(SearchRequestBuilder builder) {
        builder.setSize(0);
        final SearchResponse r = builder.get();
        return r.getHits().getTotalHits();
    }

    /**
     * Index the given IndexRequestBuilder and return the document id.
     * @param builder
     * @return
     */
    public String index(IndexRequestBuilder builder) {
        return builder.get().getId();
    }
}
