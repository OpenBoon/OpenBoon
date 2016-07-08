package com.zorroa.archivist.web.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.AnalystService;
import com.zorroa.archivist.service.AssetService;
import com.zorroa.archivist.service.NoteService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.util.Json;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
public class AssetController {

    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

    @Value("${zorroa.cluster.index.alias}")
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

    /**
     * Stream the given asset ID.
     *
     * @param response
     * @return
     * @throws ExecutionException
     * @throws FileNotFoundException
     */
    @RequestMapping(value = "/api/v1/assets/{id}/_stream", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<FileSystemResource> streamAsset(@PathVariable String id, HttpServletResponse response) throws ExecutionException, IOException {
        Asset asset = assetService.get(id);
        if (!SecurityUtils.hasPermission("export", asset)) {
            throw new AccessDeniedException("export access denied");
        }

        File path = new File(asset.getAttr("source.path", String.class));
        if (!path.exists()) {
            response.sendRedirect(asset.getAttr("source:remoteSourceUri"));
            return null;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(asset.getAttr("source.type", String.class)))
                .contentLength(asset.getAttr("source.fileSize", Long.class))
                .body(new FileSystemResource(path));
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
    public Map<String, Object> update(@RequestBody Map<String, Object> attrs, @PathVariable String id) throws IOException {
        long version = assetService.update(id, attrs);
        return ImmutableMap.of(
                "assetId", id,
                "version", version,
                "source", attrs);
    }
}
