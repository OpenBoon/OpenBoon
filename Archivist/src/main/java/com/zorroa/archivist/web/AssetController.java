package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.RoomService;
import com.zorroa.archivist.service.UserService;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class AssetController {

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Autowired
    FolderService folderService;

    // Parse the query string, converting the optional "folder" argument into a set
    // of filters and queries which are merged with the optional filter and primary query.
    // Folders are either a "should" search or a filter that restricts the output to
    // items that have at least one of the folder ids in the asset's "folder" list.
    QueryBuilder buildFolderQuery(Map<String, Object> json) {
        if (json.get("query") == null)
            return null;
        Map<String, Object> query = (Map<String, Object>) json.get("query");
        json.remove("query");   // Leave aggs, size, from, & sort for extraSource
        if (query.get("filtered") == null)
            return null;
        query = (Map<String, Object>) query.get("filtered");
        if (query.get("folder") == null)
            return null;
        Folder folder = folderService.get((String) query.get("folder"));
        byte[] primaryQueryBytes = Json.serialize(query.get("query"));
        QueryBuilder primaryQuery = QueryBuilders.wrapperQuery(primaryQueryBytes, 0, primaryQueryBytes.length);
        QueryBuilder queryBuilder = primaryQuery;       // Final combined filtered query, default to primary alone
        // Get all the decendents of the folder and create filter & query lists
        ArrayList<String> folderIds = new ArrayList<>();
        BoolQueryBuilder folderQuery = QueryBuilders.boolQuery().minimumNumberShouldMatch(1);
        List<Folder> decendents = folderService.getAllDecendents(folder);
        decendents.add(folder);
        for (Folder child : decendents) {
            if (child.getQuery() != null) {
                folderQuery.should(QueryBuilders.wrapperQuery(child.getQuery()));
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
                .setTypes("asset");
        if (search != null)
            builder.setExtraSource(search.getBytes());
        if (queryBuilder != null)
            builder.setQuery(queryBuilder);
        return builder;
    }

    @RequestMapping(value="/api/v1/assets/_search", method=RequestMethod.POST)
    public void search(@RequestBody String query, HttpSession httpSession, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Session session = userService.getSession(httpSession);
        Room room = roomService.getActiveRoom(session);
        roomService.sendToRoom(room, new Message(MessageType.ASSET_SEARCH, query));

        SearchRequestBuilder builder = buildSearch(query);
        SearchResponse response = builder.get();
        OutputStream out = httpResponse.getOutputStream();
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }

    @RequestMapping(value="/api/v1/assets/_aggregations", method=RequestMethod.POST)
    public void aggregate(@RequestBody String query, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        SearchRequestBuilder builder = buildSearch(query)
                .setSearchType(SearchType.COUNT);

        SearchResponse response = builder.get();
        OutputStream out = httpResponse.getOutputStream();
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }

    @RequestMapping(value="/api/v1/assets/_count", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String count(@RequestBody String query) throws IOException {
        Map<String, Object> json = Json.Mapper.readValue(query, new TypeReference<Map<String, Object>>() {});
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

    @RequestMapping(value="/api/v1/assets/_suggest", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String suggest(@RequestBody String query) throws IOException {
        SuggestRequestBuilder builder = client.prepareSuggest(alias);
        Map<String, Object> json = Json.Mapper.readValue(query,
                new TypeReference<Map<String, Object>>() {});
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
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Session session = userService.getSession(httpSession);
        Room room = roomService.getActiveRoom(session);
        roomService.sendToRoom(room, new Message(MessageType.ASSET_GET, id));

        GetResponse response = client.prepareGet(alias, "asset", id).get();
        OutputStream out = httpResponse.getOutputStream();

        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }

    @RequestMapping(value = "/api/v1/assets/{id}/_folders", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String update(@RequestBody String body, @PathVariable String id) throws Exception {
        // Add the request body array of collection names to the folders field
        String doc = "{\"folders\":" + body + "}";  // Hand-coded JSON doc update
        UpdateRequestBuilder builder = client.prepareUpdate(alias, "asset", id)
                .setDoc(doc)
                .setRefresh(true);  // Make sure we block until update is finished
        UpdateResponse response = builder.get();
        return new StringBuilder(128)
                .append("{\"created\":")
                .append(response.isCreated())
                .append(",\"version\":")
                .append(response.getVersion())
                .append("}")
                .toString();
    }
}
