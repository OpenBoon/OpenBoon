package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.MalformedDataException;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.util.concurrent.UncategorizedExecutionException;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    private NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

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

    private IndexRequestBuilder buildRequest(AssetBuilder builder, OpType opType) {
        try {
            return client.prepareIndex(alias, getType())
                    .setId(uuidGenerator.generate(builder.getAbsolutePath()).toString())
                    .setOpType(opType)
                    .setSource(Json.serializeToString(builder.getDocument()));
        } catch (Exception e) {
            throw new MalformedDataException(
                    "Failed to serialize object, unexpected " + e, e);
        }
    }

    @Override
    public Asset create(AssetBuilder builder) {
        builder.buildKeywords();
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

    @Override
    public void addToExport(Asset asset, Export export) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript("asset_append_export",
                ScriptService.ScriptType.INDEXED);
        updateBuilder.addScriptParam("exportId", export.getId());
        updateBuilder.get();
    }

    @Override
    public void addToFolder(Asset asset, Folder folder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), asset.getId());
        updateBuilder.setScript("asset_append_folder",
                ScriptService.ScriptType.INDEXED);
        updateBuilder.addScriptParam("folderId", folder.getId());
        updateBuilder.get();
    }

    @Override
    public boolean update(String assetId, AssetUpdateBuilder builder) {
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), assetId)
                .setDoc(builder.getSource())
                .setRefresh(true);
        UpdateResponse response = updateBuilder.get();
        return response.getVersion() > 1;   // FIXME: Need to check for version increment, not just >1? 0 == first create
    }

    @Override
    public boolean select(String assetId, boolean selected) {
        Room room = roomService.getActiveRoom(userService.getActiveSession());
        UpdateRequestBuilder updateBuilder = client.prepareUpdate(alias, getType(), assetId)
                .setScript("if (ctx._source.selectedRooms == null ) {  ctx._source.selectedRooms = [roomId] } else { if (selected) { ctx._source.selectedRooms += roomId } else { ctx._source.selectedRooms -= roomId }}",
                        ScriptService.ScriptType.INLINE)
                .addScriptParam("roomId", room.getId())
                .addScriptParam("selected", selected);
        UpdateResponse response = updateBuilder.get();
        return response.getVersion() > 1;    // FIXME: Need to check for version increment, not just >1? 0 == first create
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
