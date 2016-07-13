package com.zorroa.common.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
import com.zorroa.sdk.domain.Folder;
import com.zorroa.sdk.processor.Source;
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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    @Override
    public String getType() {
        return "asset";
    }

    private static final JsonRowMapper<Asset> MAPPER = (id, version, source) -> {
        Map<String, Object> data = Json.deserialize(source, new TypeReference<Map<String, Object>>() {});
        Asset result = new Asset();
        result.setId(id);
        result.setDocument(data);
        return result;
    };

    @Override
    public Asset index(Source source) {
        String id = source.getId();
        UpdateRequestBuilder upsert = prepareUpsert(source, id);
        return new Asset(upsert.get().getId(), source.getDocument());
    }

    @Override
    public AssetIndexResult index(String type, List<Source> sources) {
        AssetIndexResult result = new AssetIndexResult();
        if (sources.isEmpty()) {
            return result;
        }
        List<Source> retries = Lists.newArrayList();

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Source source : sources) {
            String id = source.getId();
            bulkRequest.add(prepareUpsert(source, id));
        }

        BulkResponse bulk = bulkRequest.get();

        int index = 0;
        for (BulkItemResponse response : bulk) {
            UpdateResponse update = response.getResponse();
            if (response.isFailed()) {
                String message = response.getFailure().getMessage();
                Source asset = sources.get(index);
                if (removeBrokenField(asset, message)) {
                    result.warnings++;
                    retries.add(sources.get(index));
                } else {
                    result.logs.add(new StringBuilder(1024).append(
                            message).append(",").append(asset.getPath()).toString());
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
            result.add(index(retries));
        }
        return result;
    }

    @Override
    public AssetIndexResult index(List<Source> sources) {
        return index("asset", sources);
    }

    private UpdateRequestBuilder prepareUpsert(Source source, String id) {
        byte[] doc = Json.serialize(source.getDocument());
        return client.prepareUpdate(alias, "asset", id)
                .setDoc(doc)
                .setId(id)
                .setUpsert(doc);
    }

    private UpdateRequestBuilder prepareUpsert(Source source, String id, String type) {
        byte[] doc = Json.serialize(source.getDocument());
        return client.prepareUpdate(alias, type, id)
                .setDoc(doc)
                .setId(id)
                .setUpsert(doc);
    }


    private static final Pattern[] RECOVERABLE_BULK_ERRORS = new Pattern[] {
            Pattern.compile("^MapperParsingException\\[failed to parse \\[(.*?)\\]\\];"),
            Pattern.compile("\"term in field=\"(.*?)\"\"")
    };

    private boolean removeBrokenField(Source asset, String error) {
        for (Pattern pattern: RECOVERABLE_BULK_ERRORS) {
            Matcher matcher = pattern.matcher(error);
            if (matcher.find()) {
                return asset.removeAttr(matcher.group(1));
            }
        }
        return false;
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
    public long update(String assetId, Map<String, Object> values) {
        Asset asset = get(assetId);
        for (Map.Entry<String,Object> entry: values.entrySet()) {
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
    public boolean exists(Path path) {
        return client.prepareSearch(alias)
                .setQuery(QueryBuilders.termQuery("source.path.raw", path.toString()))
                .setSize(0)
                .get().getHits().getTotalHits() > 0;
    }

    @Override
    public Asset get(Path path) {
        List<Asset> assets = elastic.query(client.prepareSearch(alias)
                .setTypes(getType())
                .setSize(1)
                .setQuery(QueryBuilders.termQuery("source.path.raw", path.toString())), MAPPER);

        if (assets.isEmpty()) {
            return null;
        }
        return assets.get(0);
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
