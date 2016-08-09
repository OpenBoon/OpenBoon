package com.zorroa.common.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    private NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    public AssetDaoImpl(String alias) {
        this.alias = alias;
    }

    @Override
    public String getType() {
        return "asset";
    }

    private static final JsonRowMapper<Asset> MAPPER = (id, version, source) -> {
        Map<String, Object> data = Json.deserialize(source, new TypeReference<Map<String, Object>>() {});
        Asset result = new Asset();
        result.setId(id);
        result.setVersion(version);
        result.setDocument(data);
        return result;
    };

    @Override
    public Asset upsert(AssetBuilder builder) {
        String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
        UpdateRequestBuilder upsert = prepareUpsert(builder, id);
        upsert.setRefresh(true);
        upsert.get();
        return get(id);
    }

    @Override
    public String upsertAsync(AssetBuilder builder) {
        String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
        UpdateRequestBuilder upsert = prepareUpsert(builder, id);
        upsert.execute();
        return id;
    }

    @Override
    public AnalyzeResult bulkUpsert(List<AssetBuilder> builders) {
        AnalyzeResult result = new AnalyzeResult();
        if (builders.isEmpty()) {
            return result;
        }
        List<AssetBuilder> retries = Lists.newArrayList();

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (AssetBuilder builder : builders) {
            String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
            bulkRequest.add(prepareUpsert(builder, id));
        }

        BulkResponse bulk = bulkRequest.get();

        int index = 0;
        for (BulkItemResponse response : bulk) {
            UpdateResponse update = response.getResponse();
            if (response.isFailed()) {
                String message = response.getFailure().getMessage();
                AssetBuilder asset = builders.get(index);
                if (removeBrokenField(asset, message)) {
                    result.warnings++;
                    retries.add(builders.get(index));
                } else {
                    result.logs.add(new StringBuilder(1024).append(
                            message).append(",").append(asset.getAbsolutePath()).toString());
                    result.errors++;
                }
            } else if (update.isCreated()) {
                result.created++;
            } else {
                result.updated++;
            }
            index++;
        }

        /*
         * TODO: limit number of retries to reasonable number.
         */
        if (!retries.isEmpty()) {
            result.retries++;
            result.add(bulkUpsert(retries));
        }
        return result;
    }

    private UpdateRequestBuilder prepareUpsert(AssetBuilder builder, String id) {
        /**
         * Close the AssetBuilder which has an open file handle to the asset itself.
         */
        builder.close();
        builder.buildKeywords();

        byte[] doc = Json.serialize(builder.getDocument());
        return client.prepareUpdate(alias, "asset", id)
                .setDoc(doc)
                .setId(id)
                .setUpsert(doc);
    }

    private static final Pattern[] RECOVERABLE_BULK_ERRORS = new Pattern[] {
            Pattern.compile("^MapperParsingException\\[failed to parse \\[(.*?)\\]\\];"),
            Pattern.compile("\"term in field=\"(.*?)\"\""),
            Pattern.compile("mapper \\[(.*?)\\] of different type")
    };

    private boolean removeBrokenField(AssetBuilder asset, String error) {
        for (Pattern pattern: RECOVERABLE_BULK_ERRORS) {
            Matcher matcher = pattern.matcher(error);
            if (matcher.find()) {
                return asset.removeAttr(matcher.group(1));
            }
        }
        return false;
    }

    @Override
    public void addToExport(Asset asset, Export export) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript(new Script("asset_append_export",
                ScriptService.ScriptType.INDEXED, "groovy",
                ImmutableMap.of("exportId", export.getId())));
        updateBuilder.setRefresh(true).get();
    }

    @Override
    public int addToFolder(Folder folder, List<String> assetIds) {
        int result = 0;

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (String id: assetIds) {
            UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), id);
            updateBuilder.setScript(new Script("asset_append_folder",
                    ScriptService.ScriptType.INDEXED, "groovy",
                    ImmutableMap.of("folderId", folder.getId())));
            bulkRequest.add(updateBuilder);
        }

        BulkResponse bulk = bulkRequest.setRefresh(true).get();
        for (BulkItemResponse rsp:  bulk.getItems()) {
            if (!rsp.isFailed()) {
                result++;
            }
        }
        return result;
    }

    @Override
    public int removeFromFolder(Folder folder, List<String> assetIds) {
        int result = 0;

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (String id: assetIds) {
            UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), id);
            updateBuilder.setScript(new Script("asset_remove_folder",
                    ScriptService.ScriptType.INDEXED, "groovy",
                    ImmutableMap.of("folderId", folder.getId())));
            bulkRequest.add(updateBuilder);
        }

        BulkResponse bulk = bulkRequest.setRefresh(true).get();
        for (BulkItemResponse rsp:  bulk.getItems()) {
            if (!rsp.isFailed()) {
                result++;
            }
        }
        return result;
    }

    @Override
    public long update(String assetId, AssetUpdateBuilder builder) {
        Asset asset = get(assetId);
        for (Map.Entry<String,Object> entry: builder.entrySet()) {
            asset.setAttr(entry.getKey(), entry.getValue());
        }

        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), assetId)
            .setDoc(Json.serializeToString(asset.getDocument()))
            .setRefresh(true);

        UpdateResponse response = updateBuilder.get();
        return response.getVersion();
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
    public Asset getByPath(String path) {
        List<Asset> assets = elastic.query(client.prepareSearch(alias).setTypes("asset").setQuery(
                QueryBuilders.termQuery("source.path.raw", path)), MAPPER);
        if (assets.isEmpty()) {
            return null;
        }
        return assets.get(0);
    }

    @Override
    public boolean existsByPathAfter(String path, long afterTime) {
        long count = client.prepareCount(alias)
                .setQuery(QueryBuilders.filteredQuery(
                        QueryBuilders.termQuery("source.path.raw", path),
                        QueryBuilders.rangeQuery("_timestamp").gt(afterTime)))
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

    @Override
    public void refresh() {
        client.admin().indices().prepareRefresh(alias).get();
    }
}
