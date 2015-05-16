package com.zorroa.archivist.repository;

import java.util.List;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.springframework.dao.EmptyResultDataAccessException;

import com.google.common.collect.Lists;

public class ElasticTemplate {

    private Client client;
    private String index;
    private String type;

    public ElasticTemplate(Client client, String index, String type) {
        this.client = client;
        this.index = index;
        this.type = type;
    }

    public <T> T queryForObject(String id, RowMapper<T> mapper) {
        final GetResponse r = client.prepareGet(index, type, id).get();
        return mapper.mapRow(r.getId(), r.getVersion(), r.getSourceAsBytes());
    }

    public <T> T queryForObject(String id, JsonRowMapper<T> mapper) {
        final GetResponse r = client.prepareGet(index, type, id).get();
        return mapper.mapRow(r.getId(), r.getVersion(), r.getSourceAsBytes());
    }

    public <T> T queryForObject(SearchRequestBuilder builder, JsonRowMapper<T> mapper) {
        final SearchResponse r = builder.get();
        if (r.getHits().getTotalHits() == 0) {
            throw new EmptyResultDataAccessException("Expected 1, was", 0);
        }
        SearchHit hit = r.getHits().getAt(0);
        return mapper.mapRow(hit.getId(), hit.getVersion(), hit.source());
    }

    public <T> List<T> query(SearchRequestBuilder builder, JsonRowMapper<T> mapper) {
        final SearchResponse r = builder.get();
        List<T> result = Lists.newArrayListWithCapacity((int)r.getHits().getTotalHits());

        for (SearchHit hit: r.getHits()) {
            result.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.source()));
        }
        return result;
    }
}
