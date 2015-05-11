package com.zorroa.archivist.repository;

import java.util.Map;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.CreateIngestRequest;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestState;

@Repository
public class IngestDaoImpl extends AbstractElasticDao implements IngestDao {

    private static final JsonRowMapper<Ingest> MAPPER = new JsonRowMapper<Ingest>() {
        @Override
        public Ingest mapRow(String id, byte[] bytes) {
            logger.info(new String(bytes));
            Ingest ingest = Json.deserialize(bytes, Ingest.class);
            ingest.setId(id);
            return ingest;
        }
    };

    @Override
    public Ingest create(CreateIngestRequest req) {

        IndexResponse response = client.prepareIndex(alias, getType())
                .setSource(Json.serialize(req))
                .get();

        Ingest ingest = new Ingest();
        ingest.setId(response.getId());
        ingest.setPaths(req.getPaths());
        ingest.setFileTypes(req.getFileTypes());
        return ingest;
    }

    @Override
    public Ingest getNext() {
        return elastic.queryForObject(client.prepareSearch(alias)
             .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
             .setQuery(QueryBuilders.matchAllQuery())
             .setPostFilter(FilterBuilders.termFilter("state", 0))
             .addSort("timeCreated", SortOrder.ASC)
             .setSize(1), MAPPER);
    }

    @Override
    public void setState(Ingest ingest, IngestState state) {
        Map<String, Object> doc = ImmutableMap.<String, Object>builder()
                .put("state", state.ordinal())
                .build();
        client.prepareUpdate()
            .setDoc(doc)
            .setIndex(alias)
            .setType(getType())
            .setId(ingest.getId())
            .get();
    }

    @Override
    public void start(Ingest ingest) {
        Map<String, Object> doc = ImmutableMap.<String, Object>builder()
                .put("state", IngestState.RUNNING.ordinal())
                .put("node", client.settings().get("node.name"))
                .put("timeStarted", System.currentTimeMillis())
                .build();

        client.prepareUpdate()
            .setDoc(doc)
            .setIndex(alias)
            .setType(getType())
            .setId(ingest.getId())
            .get();
    }

    @Override
    public Ingest get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public String getType() {
        return "ingest";
    }
}
