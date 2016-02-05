package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.sdk.domain.AssetAggregateBuilder;
import com.zorroa.archivist.sdk.domain.AssetSearch;
import com.zorroa.archivist.sdk.domain.AssetSuggestBuilder;
import com.zorroa.archivist.sdk.domain.AssetUpdateBuilder;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.AssetService;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.SearchService;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
public class AssetController {

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    AssetService assetService;

    @Autowired
    FolderService folderService;

    @Autowired
    SearchService searchService;

    @RequestMapping(value="/api/v2/assets/_search", method=RequestMethod.POST)
    public void search(@RequestBody AssetSearch search, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.search(search);
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v2/assets/_aggregate", method=RequestMethod.POST)
    public void aggregate(@RequestBody AssetAggregateBuilder aggregation, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.aggregate(aggregation);
        HttpUtils.writeElasticResponse(response, httpResponse);
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
    public void get(@PathVariable String id, HttpServletResponse httpResponse) throws IOException {
        GetResponse response = client.prepareGet(alias, "asset", id).get();
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> update(@RequestBody AssetUpdateBuilder builder, @PathVariable String id) throws IOException {
        long version = assetService.update(id, builder);
        return ImmutableMap.of(
                "assetId", id,
                "version", version,
                "source", builder);
    }

}
