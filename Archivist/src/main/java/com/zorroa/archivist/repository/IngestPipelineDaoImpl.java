package com.zorroa.archivist.repository;

import java.util.List;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestPipeline;

@Repository
public class IngestPipelineDaoImpl extends AbstractElasticDao implements IngestPipelineDao {

    @Override
    public String getType() {
        return "pipeline";
    }

    private static final JsonRowMapper<IngestPipeline> MAPPER = new JsonRowMapper<IngestPipeline>() {
        @Override
        public IngestPipeline mapRow(String id, long version, byte[] source) {
            IngestPipeline result = Json.deserialize(source, IngestPipeline.class);
            result.setId(id);
            result.setVersion(version);
            return result;
        }
    };

    @Override
    public String create(IngestPipelineBuilder builder) {
          IndexResponse response = client.prepareIndex(alias, getType())
                  .setSource(Json.serialize(builder))
                  .get();
          refreshIndex();
          return response.getId();
    }

    @Override
    public IngestPipeline get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public List<IngestPipeline> getAll() {
        return elastic.query(client.prepareSearch(alias)
                .setTypes(getType())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .setVersion(true), MAPPER);

    }

}
