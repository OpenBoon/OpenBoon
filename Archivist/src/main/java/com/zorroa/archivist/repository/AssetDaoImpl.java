package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.util.concurrent.UncategorizedExecutionException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    private NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    @Override
    public String getType() {
        return "asset";
    }

    private static final JsonRowMapper<Asset> MAPPER = new JsonRowMapper<Asset>() {
        @Override
        public Asset mapRow(String id, long version, byte[] source)  throws Exception {
            Map<String, Object> data;
            data = Json.Mapper.readValue(source, new TypeReference<Map<String, Object>>() {});
            Asset result = new Asset();
            result.setId(id);
            result.setVersion(version);
            result.setDocument(data);
            return result;
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
                client.admin().indices().preparePutMapping(alias)
                        .setIgnoreConflicts(true)
                        .setType(getType())
                        .setSource(mapper)
                        .execute()
                        .actionGet();
            } catch (ElasticsearchException e) {
                logger.error("Elasticsearch mapping exception for " + builder.getFilename() + ": " + e.getDetailedMessage());
                e.printStackTrace();
            } catch (IOException e) {
                logger.error("IOException while updating mapping for " + builder.getFilename() + ": " + e.getMessage());
                e.printStackTrace();
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

    private String fieldFromError(String error, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(error);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean removeFieldAndRetryReplace(AssetBuilder builder, String error, String regex) {
        String field = fieldFromError(error, regex);                // E.g Exif.makerTag
        if (field != null) {
            int idx = field.indexOf('.');                           // Use indexOf to handle 3+ fields
            if (idx > 0 && idx < field.length() - 1) {
                String namespace = field.substring(0, idx);         // E.g. Exif
                String key = field.substring(idx + 1);              // E.g. makerTag
                Object oldValue = builder.remove(namespace, key);
                if (oldValue != null) {
                    builder.put("source", "warning", field);
                    return replace(builder);
                }
            }
        }
        return false;
    }

    @Override
    public boolean replace(AssetBuilder builder) {
        try {
            IndexRequestBuilder idxBuilder = buildRequest(builder, OpType.INDEX);
            return !idxBuilder.get().isCreated();
        } catch (MapperParsingException e) {
            logger.error("Elasticsearch mapper parser error indexing " + builder.getFilename() + ": " + e.getDetailedMessage());
            return removeFieldAndRetryReplace(builder, e.getDetailedMessage(), "failed to parse \\[(.*?)\\]");
        } catch (UncategorizedExecutionException e) {
            logger.error("Uncategorized execution error indexing " + builder.getFilename() + ": " + e.getDetailedMessage());
            return removeFieldAndRetryReplace(builder, e.getDetailedMessage(), "term in field=\"(.*?)\"");
        } catch (ElasticsearchException e) {
            logger.error("Elasticsearch error indexing " + builder.getFilename() + ": " + e.getDetailedMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument error indexing " + builder.getFilename() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void addToExport(Asset asset, Export export) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript(
                "if (ctx._source.exports == null ) {  ctx._source.exports = [exportId] } else { ctx._source.exports += exportId }",
                ScriptService.ScriptType.INLINE);
        updateBuilder.addScriptParam("exportId", export.getId());
        updateBuilder.get();
    }

    @Override
    public void addToFolder(Asset asset, Folder folder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript(
                "if (ctx._source.folders == null ) {  ctx._source.folders = [folderId] } else { ctx._source.foldrs += folderId }",
                ScriptService.ScriptType.INLINE);
        updateBuilder.addScriptParam("folderId", folder.getId());
        updateBuilder.get();
    }

    @Override
    public boolean update(String assetId, AssetUpdateBuilder builder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), assetId)
                .setDoc(builder.getSource())
                .setRefresh(true);
        UpdateResponse response = updateBuilder.get();
        return response.getVersion() > 1;
    }

    @Override
    public Asset get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public boolean existsByPath(String path) {
        long count = client.prepareCount(alias)
                .setQuery(QueryBuilders.termQuery("source.path.raw", path))
                .get()
                .getCount();
        return count > 0;
    }

    @Override
    public boolean existsByPathAfter(String path, long afterTime) {
        long count = client.prepareCount(alias)
                .setQuery(QueryBuilders.filteredQuery(
                        QueryBuilders.termQuery("source.path.raw", path),
                        FilterBuilders.rangeFilter("_timestamp").gt(afterTime)))
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
