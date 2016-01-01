package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    private NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    @Override
    public String getType() {
        return "asset";
    }

    private static final JsonRowMapper<Asset> MAPPER = (id, version, source) -> {
        Map<String, Object> data;
        data = Json.deserialize(source, new TypeReference<Map<String, Object>>() {
        });
        Asset result = new Asset();
        result.setId(id);
        result.setVersion(version);
        result.setDocument(data);
        return result;
    };

    private UpdateRequestBuilder buildRequest(AssetBuilder builder, String id) {
        builder.buildKeywords();
        byte[] doc = Json.serialize(builder.getDocument());
        return client.prepareUpdate(alias, getType(), id)
                .setDoc(doc)
                .setId(id)
                .setUpsert(doc);
    }

    @Override
    public Asset upsert(AssetBuilder builder) {
        String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
        UpdateRequestBuilder upsert = buildRequest(builder, id);
        upsert.setRefresh(true);
        upsert.get();
        return get(id);
    }

    @Override
    public String upsertAsync(AssetBuilder builder) {
        String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
        UpdateRequestBuilder upsert = buildRequest(builder, id);
        upsert.execute();
        return id;
    }

    @Override
    public String upsertAsync(AssetBuilder builder, ActionListener<UpdateResponse> listener) {
        String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
        UpdateRequestBuilder upsert = buildRequest(builder, id);
        upsert.execute(listener);
        return id;
    }

    @Override
    public BulkResponse bulkUpsert(List<AssetBuilder> builders) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (AssetBuilder builder: builders) {
            String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
            bulkRequest.add(buildRequest(builder, id));
        }
        return bulkRequest.get();
    }

    /*

    These are commented out until we figure out what they are used for.  There
    might be a better way to reocover from the problem.

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
                    builder.put("warnings", "field_warning", field);
                    return replace(builder);
                }
            }
        }
        return false;
    }

    @Override
    public boolean replace(AssetBuilder builder) {
        builder.buildKeywords();
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
    */

    @Override
    public void addToExport(Asset asset, Export export) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript("asset_append_export",
                ScriptService.ScriptType.INDEXED);
        updateBuilder.addScriptParam("exportId", export.getId());
        updateBuilder.setRefresh(true).get();
    }

    @Override
    public void addToFolder(Asset asset, Folder folder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript("asset_append_folder",
                ScriptService.ScriptType.INDEXED);
        updateBuilder.addScriptParam("folderId", folder.getId());
        updateBuilder.setRefresh(true).get();
    }

    @Override
    public void removeFromFolder(Asset asset, Folder folder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript("asset_remove_folder",
                ScriptService.ScriptType.INDEXED);
        updateBuilder.addScriptParam("folderId", folder.getId());
        updateBuilder.setRefresh(true).get();
    }

    @Deprecated
    @Override
    public long setFolders(Asset asset, Collection<Folder> folders) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setDoc(ImmutableMap.of("folders", folders.stream().map(
                Folder::getId).collect(Collectors.toSet())))
                .setRefresh(true);
        return updateBuilder.setRefresh(true).get().getVersion();
    }

    @Override
    public long update(String assetId, AssetUpdateBuilder builder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), assetId)
                .setDoc(builder.getSource())
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

    @Override
    public void refresh() {
        client.admin().indices().prepareRefresh(alias).get();
    }
}
