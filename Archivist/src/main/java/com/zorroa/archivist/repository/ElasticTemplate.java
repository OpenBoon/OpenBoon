package com.zorroa.archivist.repository;

import org.elasticsearch.client.Client;

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
        return mapper.mapRow(client.prepareGet(index, type, id).get().getSourceAsMap());
    }

    public <T> T queryForObject(String id, JsonRowMapper<T> mapper) {
        return mapper.mapRow(client.prepareGet(index, type, id).get().getSourceAsBytes());
    }
}
