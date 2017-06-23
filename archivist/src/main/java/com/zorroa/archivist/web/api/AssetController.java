package com.zorroa.archivist.web.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.archivist.web.MultipartFileSender;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.ProxySchema;
import com.zorroa.sdk.search.AssetAggregateBuilder;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.search.AssetSuggestBuilder;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.ResourceNotFoundException;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
    EventLogService logService;

    @Autowired
    ImageService imageService;

    @Autowired
    ObjectFileSystem ofs;

    @Autowired
    CommandService commandService;

    /**
     * Describes a file to stream.
     */
    private static class StreamFile {
        public String path;
        public String mimeType;
        public boolean proxy;

        public StreamFile(String path, String mimeType, boolean proxy) {
            this.path = path;
            this.mimeType = mimeType;
            this.proxy = proxy;
        }
    }

    /**
     * Ability to make certain file extensions to other, more
     * web supported alternatives.
     */
    private static final Map<String, List<List<String>>>
            PREFERRED_FORMATS = ImmutableMap.of("mov",
                ImmutableList.of(
                        ImmutableList.of("mp4", "video/mp4"),
                        ImmutableList.of("ogv", "video/ogg")));

    /**
     * We could try to detect it using something like Tika but
     * there are only a couple types.
     */
    private static final Map<String,String> PROXY_MIME_LOOKUP =
            ImmutableMap.of("png", "image/png",
                            "jpg", "image/jpeg");

    public StreamFile getPreferredFormat(Asset asset, boolean streamProxy) {
        if (streamProxy) {
            return getProxyStream(asset);
        }
        else {
            String path = asset.getAttr("source.path", String.class);
            String ext = FileUtils.extension(path);
            List<List<String>> preferred = PREFERRED_FORMATS.get(ext);
            if (preferred != null) {
                for (List<String> e : preferred) {
                    String newPath = path.substring(0, path.length() - (ext.length() + 1)) + "." + e.get(0);
                    if (new File(newPath).exists()) {
                        return new StreamFile(newPath, e.get(1), false);
                    }
                }
            }

            if (new File(path).exists()) {
                return new StreamFile(path,
                        asset.getAttr("source.mediaType", String.class), false);
            } else {
                return getProxyStream(asset);
            }
        }
    }

    public StreamFile getProxyStream(Asset asset) {
        Proxy largestProxy = asset.getProxies().getLargest();
        return new StreamFile(
                ofs.get(largestProxy.getId()).getFile().toString(),
                PROXY_MIME_LOOKUP.getOrDefault(largestProxy.getFormat(),
                        "application/octet-stream"), true);
    }

    @RequestMapping(value = "/api/v1/assets/{id}/_stream", method = RequestMethod.GET)
    public void streamAsset(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Asset asset = assetService.get(id);
        StreamFile format = getPreferredFormat(asset,
                !SecurityUtils.hasPermission("export", asset));
        logService.logAsync(UserLogSpec.build(LogAction.View, "asset", asset.getId()));

        try {
            MultipartFileSender.fromPath(Paths.get(format.path))
                    .with(request)
                    .with(response)
                    .setContentType(format.mimeType)
                    .serveResource();
        } catch (Exception e) {
            logger.warn("MultipartFileSender failed on {}, unexpected {}", id, e.getMessage());
        }
    }

    @RequestMapping(value="/api/v1/assets/{id}/notes", method=RequestMethod.GET)
    public List<Note> getNotes(@PathVariable String id) throws IOException {
        return noteService.getAll(id);
    }

    @RequestMapping(value="/api/v1/assets/{id}/proxies/closest/{size:\\d+x\\d+}", method=RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getClosestProxy(@PathVariable String id, @PathVariable(required=false) String size) throws IOException {
        try {
            String[] wh = size.split("x");
            ProxySchema proxies = assetService.get(id).getProxies();
            Proxy proxy = proxies.getClosest(Integer.valueOf(wh[0]), Integer.valueOf(wh[1]));
            return imageService.serveImage(proxy);
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @RequestMapping(value="/api/v1/assets/{id}/proxies/largest", method=RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getLargestProxy(@PathVariable String id) throws IOException {
        try {
            ProxySchema proxies = assetService.get(id).getProxies();
            Proxy proxy = proxies.getLargest();
            return imageService.serveImage(proxy);
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @RequestMapping(value="/api/v1/assets/{id}/proxies/smallest", method=RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getSmallestProxy(@PathVariable String id) throws IOException {
        try {
            ProxySchema proxies = assetService.get(id).getProxies();
            Proxy proxy = proxies.getSmallest();
            return imageService.serveImage(proxy);
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
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

    @RequestMapping(value="/api/v4/assets/_search", method=RequestMethod.POST)
    public void searchV4(@RequestBody AssetSearch search, ServletOutputStream out) throws IOException {
        searchService.search(new Pager(search.getFrom(), search.getSize(), 0), search, out);
    }

    @RequestMapping(value="/api/v1/assets/_fields", method=RequestMethod.GET)
    public Map<String, Set<String>> getFields() throws IOException {
        return searchService.getFields();
    }

    @RequestMapping(value="/api/v1/assets/_mapping", method=RequestMethod.GET)
    public Map<String,Object> getMapping() throws IOException {
        return assetService.getMapping();
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

    @RequestMapping(value="/api/v1/assets/{id}/_exists", method=RequestMethod.GET)
    public Object exists(@PathVariable String id) throws IOException {
        return HttpUtils.exists(id, assetService.exists(id));
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


    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.DELETE)
    public Object delete(@PathVariable String id) throws IOException {
        Asset asset = assetService.get(id);
        if (!SecurityUtils.hasPermission("write", asset)) {
            throw new ArchivistWriteException("delete access denied");
        }

        boolean result = assetService.delete(id);
        return HttpUtils.deleted("asset", id, result);
    }

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
    public Object update(@RequestBody Map<String, Object> attrs, @PathVariable String id) throws IOException {
        Asset asset = assetService.get(id);
        if (!SecurityUtils.hasPermission("write", asset)) {
            throw new ArchivistWriteException("update access denied");
        }


        assetService.update(id, attrs);
        return HttpUtils.updated("asset", id, true, assetService.get(id));
    }

    @RequestMapping(value="/api/v1/assets/{id}/_fields", method=RequestMethod.DELETE)
    public Object removeFields(@RequestBody Set<String> fields, @PathVariable String id) throws IOException {
        assetService.removeFields(id, fields);
        return HttpUtils.updated("asset", id, true, ImmutableMap.of());
    }

    public static class IndexAssetRequest {
        public List<Source> sources;
        public LinkSpec link;
    }

    @RequestMapping(value="/api/v1/assets/_index", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public DocumentIndexResult index(@RequestBody IndexAssetRequest req) throws IOException {
        return assetService.index(req.sources, req.link);
    }

    public static class SetPermissionsRequest {
        public AssetSearch search;
        public Acl acl;

    }

    @PreAuthorize("hasAuthority('group::share') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/assets/_permissions", method=RequestMethod.PUT)
    public Command setPermissions(
            @Valid @RequestBody SetPermissionsRequest req) throws Exception {
        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.UpdateAssetPermissions);
        spec.setArgs(new Object[] {
                req.search,
                req.acl
        });
        return commandService.submit(spec);
    }

    @RequestMapping(value="/api/v1/refresh", method=RequestMethod.PUT)
    public void refresh() {
        ElasticClientUtils.refreshIndex(client, 0);
    }
}
