package com.zorroa.archivist.repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.IngestPipeline;

@Repository
public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    @Autowired
    Client elasticSearchClient;

    @Override
    public String getType() {
        return "asset";
    }

    private static final JsonRowMapper<Asset> MAPPER = new JsonRowMapper<Asset>() {
        @Override
        public Asset mapRow(String id, long version, byte[] source) {
            Map<String, Object> data;
            try {
                data = Json.Mapper.readValue(source, new TypeReference<Map<String, Object>>() {});
                Asset result = new Asset();
                result.setId(id);
                result.setVersion(version);
                result.setData(data);
                return result;
            } catch (IOException e) {
                throw new DataRetrievalFailureException("Failed to parse asset record, " + e, e);
            }
        }
    };

    @Override
    public String create(AssetBuilder builder) {
          IndexRequestBuilder idxBuilder = client.prepareIndex(alias, getType())
                  .setSource(Json.serialize(builder.getDocument()));
          if (builder.isAsync()) {
              idxBuilder.execute();
              return null;
          }
          else {
              String id = idxBuilder.get().getId();
              refreshIndex();
              return id;
          }
    }

    @Override
    public Asset get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public List<Asset> getAll() {
        return elastic.query(client.prepareSearch(alias)
                .setTypes(getType())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .setVersion(true), MAPPER);

    }
}
