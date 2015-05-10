package com.zorroa.archivist.repository;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.CreateIngestRequest;

@Repository
public class IngestDaoImpl implements IngestDao {

    @Autowired
    Client elastic;

    @Value("${archivist.index.alias}")
    private String alias;

    private static final String TYPE = "ingest";

    @Override
    public Ingest create(CreateIngestRequest req) {
        IndexResponse response = elastic.prepareIndex(alias, TYPE).setSource(Json.serialize(req)).get();

        Ingest ingest = new Ingest();
        ingest.setId(response.getId());
        ingest.setPaths(req.getPaths());
        ingest.setFileTypes(req.getFileTypes());
        return ingest;
    }

    @Override
    public Ingest get(String id) {
        return Json.deserialize(elastic.prepareGet(alias, TYPE, id).get().getSourceAsBytes(), Ingest.class);
    }
}
