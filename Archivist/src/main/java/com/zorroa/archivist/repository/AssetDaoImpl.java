package com.zorroa.archivist.repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;

@Repository
public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    private NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

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
                result.setDocument(data);
                return result;
            } catch (IOException e) {
                throw new DataRetrievalFailureException("Failed to parse asset record, " + e, e);
            }
        }
    };

    @Override
    public Asset create(AssetBuilder builder) {
        IndexRequestBuilder idxBuilder = client.prepareIndex(alias, getType())
                .setId(uuidGenerator.generate(builder.getAbsolutePath()).toString())
                .setOpType(OpType.CREATE)
                .setSource(Json.serialize(builder.getDocument()));
        if (builder.isAsync()) {
            idxBuilder.execute();
            return null;
        }
        else {
            idxBuilder.setRefresh(true);
            String id = idxBuilder.get().getId();
            return get(id);
        }
    }

    @Override
    public void fastCreate(AssetBuilder builder) {
        IndexRequestBuilder idxBuilder = client.prepareIndex(alias, getType())
                .setId(uuidGenerator.generate(builder.getAbsolutePath()).toString())
                .setOpType(OpType.CREATE)
                .setSource(Json.serialize(builder.getDocument()));
        idxBuilder.execute();
    }

    @Override
    public Asset get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public boolean existsByPath(String path) {
        long count = client.prepareCount(alias)
                .setQuery(QueryBuilders.termQuery("source.path.untouched", path))
                .get()
                .getCount();
        return count > 0;
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
