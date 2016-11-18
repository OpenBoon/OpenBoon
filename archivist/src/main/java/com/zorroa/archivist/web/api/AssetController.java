package com.zorroa.archivist.web.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.AssetPermissionUpdate;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.archivist.web.MultipartFileSender;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetAggregateBuilder;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.search.AssetSuggestBuilder;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Autowired
    LogService logService;

    @RequestMapping(value = "/api/v1/assets/{id}/_stream", method = RequestMethod.GET)
    public void streamAsset(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Asset asset = assetService.get(id);

        if (!SecurityUtils.hasPermission("export", asset)) {
            throw new AccessDeniedException("export access denied");
        }

        logService.log(LogSpec.build(LogAction.Export, "asset", asset.getId()));
        File path = new File(asset.getAttr("source.path", String.class));

        try {
            MultipartFileSender.fromPath(path.toPath())
                    .with(request)
                    .with(response)
                    .setContentType(asset.getAttr("source.mediaType", String.class))
                    .serveResource();
        } catch (Exception e) {
            logger.warn("MultipartFileSender failed, " + id);
        }
    }

    @RequestMapping(value="/api/v1/assets/{id}/notes", method=RequestMethod.GET)
    public List<Note> getNotes(@PathVariable String id) throws IOException {
        return noteService.getAll(id);
    }

    @RequestMapping(value="/api/v2/assets/_search", method=RequestMethod.POST)
    public void searchV2(@RequestBody AssetSearch search, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SearchResponse response = searchService.search(search);
        HttpUtils.writeElasticResponse(response, httpResponse);
    }

    @RequestMapping(value="/api/v3/assets/_search", method=RequestMethod.POST)
    public PagedList<Asset> searchV3(@RequestBody AssetSearch search) throws IOException {
        return searchService.search(new Pager(search.getFrom(), search.getSize(), 0), search);
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
    public Object count(@RequestBody AssetSearch search) throws IOException {
        return HttpUtils.count(searchService.count(search));
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

    @RequestMapping(value="/api/v2/assets/{id}", method=RequestMethod.GET)
    public Object getV2(@PathVariable String id) {
        return assetService.get(id);
    }

    @RequestMapping(value="/api/v1/assets/_path", method=RequestMethod.GET)
    public Object getByPath(@RequestBody Map<String,String> path) {
        return assetService.get(path.get("path"));
    }

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
    public Object update(@RequestBody Map<String, Object> attrs, @PathVariable String id) throws IOException {
        Asset asset = assetService.get(id);
        if (!SecurityUtils.hasPermission("write", asset)) {
            throw new AccessDeniedException("export access denied");
        }


        assetService.update(id, attrs);
        return HttpUtils.updated("asset", id, true, assetService.get(id));
    }

    public static class IndexAssetRequest {
        public List<Source> sources;
        public LinkSpec link;
    }

    @RequestMapping(value="/api/v1/assets/_index", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public DocumentIndexResult index(@RequestBody IndexAssetRequest req) throws IOException {
        return assetService.index(req.sources, req.link);
    }

    /**
     * Remove a permission from a list of assets.
     *
     * @param change
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/api/v1/assets/_permissions", method=RequestMethod.DELETE)
    public Object removePermission(
            @Valid @RequestBody AssetPermissionUpdate change) throws Exception {
        return assetService.removePermission(change.getType(), change.getId(), change.getAssetIds());
    }

    /**
     * Add a permission to a list of assets.
     *
     * @param change
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/api/v1/assets/_permissions", method=RequestMethod.POST)
    public Object appendPermission(
            @Valid  @RequestBody AssetPermissionUpdate change) throws Exception {
        return assetService.appendPermission(change.getType(), change.getId(), change.getAssetIds());
    }
}
