package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.SearchService;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class AssetController {

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    AssetService assetService;

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Autowired
    FolderService folderService;

    @Autowired
    SearchService searchService;

    // Parse the query string, converting the optional "folder" argument into a set
    // of filters and queries which are merged with the optional filter and primary query.
    // Folders are either a "should" search or a filter that restricts the output to
    // items that have at least one of the folder ids in the asset's "folder" list.
    QueryBuilder buildFolderQuery(Map<String, Object> json) {
        // Determine if we have a "folder" option, returning null if not and
        // returning a QueryBuilder if we do, modifying the json map as a side effect
        if (json.get("query") == null)
            return null;
        Map<String, Object> query = (Map<String, Object>) json.get("query");
        if (query.get("filtered") == null)
            return null;
        query = (Map<String, Object>) query.get("filtered");
        if (query.get("folder") == null)
            return null;
        json.remove("query");   // Leave aggs, size, from, & sort for extraSource

        // FIXME: Instead of using the id for folderService.get, read embedded json directly (by value)
        Folder folder = folderService.get((String) ((Map<String, Object>)query.get("folder")).get("id"));
        byte[] primaryQueryBytes = Json.serialize(query.get("query"));
        QueryBuilder primaryQuery = QueryBuilders.wrapperQuery(primaryQueryBytes, 0, primaryQueryBytes.length);
        QueryBuilder queryBuilder = primaryQuery;       // Final combined filtered query, default to primary alone

        // Get all the decendents of the folder and create filter & query lists
        ArrayList<String> folderIds = new ArrayList<>();
        BoolQueryBuilder folderQuery = QueryBuilders.boolQuery().minimumNumberShouldMatch(1);
        Set<Folder> decendents = folderService.getAllDescendants(folder);
        decendents.add(folder);
        for (Folder child : decendents) {
            if (child.getSearch() != null) {
                folderQuery.should(QueryBuilders.wrapperQuery(child.getSearch().getQuery()));
            } else {
                folderIds.add(child.getId());
            }
        }

        // Merge any folder filters with the optional query filter
        FilterBuilder filterBuilder = null;     // Final combined filter
        if (folderIds.size() > 0) {
            filterBuilder = FilterBuilders.termsFilter("folders", folderIds);
        }
        FilterBuilder primaryFilter = null;
        if (query.get("filter") != null) {
            byte[] queryFilterBytes = Json.serialize(query.get("filter"));
            primaryFilter = FilterBuilders.wrapperFilter(queryFilterBytes, 0, queryFilterBytes.length);
        }
        if (filterBuilder != null && primaryFilter != null) {
            filterBuilder = FilterBuilders.boolFilter().must(primaryFilter, filterBuilder);
        } else if (primaryFilter != null) {
            filterBuilder = primaryFilter;
        }

        // Merge the folder queries & filters with primary query
        if (filterBuilder != null && folderQuery.hasClauses()) {
            folderQuery.should(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder));
            queryBuilder = QueryBuilders.boolQuery()
                    .must(primaryQuery)
                    .must(folderQuery);
        } else if (folderQuery.hasClauses()) {
            queryBuilder = QueryBuilders.boolQuery()
                    .must(primaryQuery)
                    .must(folderQuery)
                    .minimumNumberShouldMatch(1);
        } else {
            queryBuilder = QueryBuilders.filteredQuery(primaryQuery, filterBuilder);
        }

        return queryBuilder;
    }

    private SearchRequestBuilder buildSearch(String query) throws IOException {
        Map<String, Object> json = Json.Mapper.readValue(query, new TypeReference<Map<String, Object>>() {});
        QueryBuilder queryBuilder = buildFolderQuery(json);
        String search = json.size() > 0 ? new String(Json.serialize(json), StandardCharsets.UTF_8) : null;
        SearchRequestBuilder builder = client.prepareSearch(alias)
                .setTypes("asset")
                .setPostFilter(SecurityUtils.getPermissionsFilter());
        if (search != null)
            builder.setExtraSource(search.getBytes());
        if (queryBuilder != null)
            builder.setQuery(queryBuilder);
        return builder;
    }


    @RequestMapping(value="/api/v2/assets/_search", method=RequestMethod.POST)
    public void search(@RequestBody AssetSearch search, HttpSession httpSession, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.search(search);
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v1/assets/_search", method=RequestMethod.POST)
    public void search(@RequestBody String query, @RequestParam(value="roomId", defaultValue="0", required=false) int roomId, HttpSession httpSession, HttpServletResponse httpResponse) throws IOException {
        SearchRequestBuilder builder = buildSearch(query);
        SearchResponse response = builder.get();
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v2/assets/_aggregate", method=RequestMethod.POST)
    public void aggregate(@RequestBody AssetAggregateBuilder aggregation, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.aggregate(aggregation);
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v1/assets/_aggregations", method=RequestMethod.POST)
    public void aggregate(@RequestBody String query, HttpServletResponse httpResponse) throws IOException {
        SearchRequestBuilder builder = buildSearch(query)
                .setSearchType(SearchType.COUNT);
        HttpUtils.writeElasticResponse(builder.get(), httpResponse);
    }

    @RequestMapping(value="/api/v2/assets/_count", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String count(@RequestBody AssetSearch search) throws IOException {
        CountResponse response = searchService.count(search);
        return new StringBuilder(128)
                .append("{\"count\":")
                .append(response.getCount())
                .append(",\"_shards\":{\"total\":")
                .append(response.getTotalShards())
                .append(",\"successful\":")
                .append(response.getSuccessfulShards())
                .append(",\"failed\":")
                .append(response.getFailedShards())
                .append("}}")
                .toString();
    }

    @RequestMapping(value="/api/v1/assets/_count", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String count(@RequestBody String query) throws IOException {
        Map<String, Object> json = Json.Mapper.readValue(query, new TypeReference<Map<String, Object>>() {
        });
        QueryBuilder queryBuilder = buildFolderQuery(json);
        String search = json.size() > 0 ? new String(Json.serialize(json), StandardCharsets.UTF_8) : null;
        CountRequestBuilder builder = client.prepareCount(alias)
                .setTypes("asset");
        if (search != null)
            builder.setSource(search.getBytes());
        if (queryBuilder != null)
            builder.setQuery(queryBuilder);

        CountResponse response = builder.get();
        return new StringBuilder(128)
            .append("{\"count\":")
            .append(response.getCount())
            .append(",\"_shards\":{\"total\":")
            .append(response.getTotalShards())
            .append(",\"successful\":")
            .append(response.getSuccessfulShards())
            .append(",\"failed\":")
            .append(response.getFailedShards())
            .append("}}")
            .toString();
    }

    @RequestMapping(value="/api/v2/assets/_suggest", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String suggest(@RequestBody AssetSuggestBuilder search) throws IOException {
        SuggestResponse response = searchService.suggest(search);
        return response.toString();
    }

    @RequestMapping(value="/api/v1/assets/_suggest", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String suggest(@RequestBody String query) throws IOException {
        SuggestRequestBuilder builder = client.prepareSuggest(alias);
        Map<String, Object> json = Json.Mapper.readValue(query,
                new TypeReference<Map<String, Object>>() {
                });
        // Create a top-level suggestion for each top-level dictionary in the request body
        // TODO: Only handles "completion" suggestions currently
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            Map<String, Object> suggestMap = (Map<String, Object>)entry.getValue();
            String text = (String)suggestMap.get("text");
            Map<String, String> completionMap = (Map<String, String>)suggestMap.get("completion");
            String field = completionMap.get("field");
            CompletionSuggestionBuilder cbuilder = new CompletionSuggestionBuilder(entry.getKey())
                    .text(text)
                    .field(field);
            builder.addSuggestion(cbuilder);
        }
        SuggestResponse response = builder.get();
        return response.toString();
    }

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.GET)
    public void get(@PathVariable String id, HttpSession httpSession, HttpServletResponse httpResponse) throws IOException {

        Session session = userService.getActiveSession();
        Room room = roomService.getActiveRoom(session);
        roomService.sendToRoom(room, new Message(MessageType.ASSET_GET, id));

        GetResponse response = client.prepareGet(alias, "asset", id).get();
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> update(@RequestBody AssetUpdateBuilder builder, @PathVariable String id, HttpSession httpSession) throws IOException {
        long version = assetService.update(id, builder);
        return ImmutableMap.of(
                "assetId", id,
                "version", version,
                "source", builder);
    }

    @RequestMapping(value="/api/v1/assets/{id}/_folders", method=RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> setFolders(@RequestBody List<String> folder, @PathVariable String id, HttpSession httpSession) throws Exception {
        Asset asset = assetService.get(id);
        List<Folder> folders = folderService.getAll(folder);
        assetService.setFolders(asset, folders);
        return ImmutableMap.of("assigned", folders.size());
    }
}
