package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.archivist.web.exceptions.ClusterStateException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class AssetController {

    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

    @Value("${zorroa.common.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    AssetService assetService;

    @Autowired
    NoteService noteService;

    @Autowired
    SearchService searchService;

    @Autowired
    AnalystService analystService;

    @RequestMapping(value="/api/v1/assets/{id}/_stream", method=RequestMethod.GET)
    public void stream(@PathVariable String id, HttpServletResponse response) throws IOException {
        Asset asset = assetService.get(id);
        if (!SecurityUtils.hasPermission("export", asset)) {
            throw new AccessDeniedException("export access denied");
        }

        /**
         * If the 'objectStorageHost' is NULL, the asset can be streamed from any analyst, so its
         * a matter of picking one and forwarding it.
         */
        StringBuilder sb = new StringBuilder(128);
        String host = asset.getAttr("source.objectStorageHost");
        if (host == null) {

            /**
             * TODO: need to add some kind of limit, caching, or just do this better.
             * so its not per request.
             */
            List<Analyst> analysts = analystService.getActive();
            if (analysts.isEmpty()) {
                throw new ClusterStateException("No available analysts");
            }

            Analyst a = analysts.get(ThreadLocalRandom.current().nextInt(analysts.size()));
            host = a.getUrl();
        }

        sb.append(host);
        sb.append("/api/v1/assets/");
        sb.append(asset.getId());
        sb.append("/_stream");

        String uri = sb.toString();
        response.sendRedirect(uri);
    }

    @RequestMapping(value="/api/v1/assets/{id}/notes", method=RequestMethod.GET)
    public List<Note> getAllNotes(@PathVariable String id) throws IOException {
        return noteService.getAll(id);
    }

    @RequestMapping(value="/api/v2/assets/_search", method=RequestMethod.POST)
    public void search(@RequestBody AssetSearch search, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.search(search);
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v1/assets/_fields", method=RequestMethod.GET)
    public Map<String, Set<String>> getFields() throws IOException {
        return searchService.getFields();
    }

    @RequestMapping(value="/api/v2/assets/_aggregate", method=RequestMethod.POST)
    public void aggregate(@RequestBody AssetAggregateBuilder aggregation, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.aggregate(aggregation);
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v2/assets/_count", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public String count(@RequestBody AssetSearch search) throws IOException {
        return HttpUtils.countResponse(searchService.count(search));
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

    @RequestMapping(value="/api/v1/assets/_analyze", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public AnalyzeResult analyze(@RequestBody AnalyzeRequest request) throws Exception {
        Preconditions.checkNotNull(request.getAssets(), "The assets to analyze cannot be null");
        Preconditions.checkNotNull(request.getProcessors(), "The processors cannot be null");
        request.setUser(SecurityUtils.getUser().getUsername());
        return analystService.getAnalystClient().analyze(request);
    }
}
