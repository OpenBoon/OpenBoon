package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.sdk.AssetBuilder;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    private IndexRequestBuilder buildRequest(AssetBuilder builder, OpType opType) {
        if (builder.getMapping().size() > 0) {
            try {
                XContentBuilder mapper = XContentFactory.jsonBuilder().startObject()
                        .startObject(getType()).startObject("properties");
                for (Map.Entry<String, Object> entry : builder.getMapping().entrySet()) {
                    mapper = mapper.startObject(entry.getKey());
                    Map<String, Object> namespaceMap = (Map<String, Object>) entry.getValue();
                    mapper = mapper.field("properties", namespaceMap).endObject();
                }
                mapper = mapper.endObject().endObject().endObject();
                client.admin().indices().preparePutMapping(alias).setType(getType())
                        .setSource(mapper).execute().actionGet();
                builder.updateMapped();
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to map asset record, " + e, e);
            }
        }
        return client.prepareIndex(alias, getType())
                .setId(uuidGenerator.generate(builder.getAbsolutePath()).toString())
                .setOpType(opType)
                .setSource(Json.serialize(builder.getDocument()));

    }
    @Override
    public Asset create(AssetBuilder builder) {
        IndexRequestBuilder idxBuilder = buildRequest(builder, OpType.CREATE);
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
        IndexRequestBuilder idxBuilder = buildRequest(builder, OpType.INDEX);
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
