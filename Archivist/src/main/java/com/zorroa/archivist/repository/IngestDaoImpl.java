package com.zorroa.archivist.repository;

import org.elasticsearch.action.index.IndexResponse;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.CreateIngestRequest;
import com.zorroa.archivist.domain.Ingest;

@Repository
public class IngestDaoImpl extends AbstractElasticDao implements IngestDao {

    private static final JsonRowMapper<Ingest> MAPPER = new JsonRowMapper<Ingest>() {
        @Override
        public Ingest mapRow(String id, byte[] bytes) {
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
    public Ingest get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public String getType() {
        return "ingest";
    }
}
