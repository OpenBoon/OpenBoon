package com.zorroa.archivist.web.api;

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
import com.zorroa.sdk.schema.ProxySchema;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.search.AssetSuggestBuilder;
import org.apache.tika.Tika;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
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

    private static final Tika tika = new Tika();

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
     * We could try to detect it using something like Tika but
     * there are only a couple types.
     */
    private static final Map<String,String> PROXY_MIME_LOOKUP =
            ImmutableMap.of("png", "image/png",
                            "jpg", "image/jpeg");

    public StreamFile getPreferredFormat(Asset asset, String preferExt, boolean fallback, boolean streamProxy) {
        if (streamProxy) {
            return getProxyStream(asset);
        }
        else {
            String path = asset.getAttr("source.path", String.class);
            String mediaType = asset.getAttr("source.mediaType", String.class);

            if (preferExt != null) {
                path = path.substring(0, path.lastIndexOf('.')+1) + preferExt;
                mediaType = tika.detect(path);
            }

            if (new File(path).exists()) {
                return new StreamFile(path, mediaType,false);
            } else {
                if (fallback) {
                    return getProxyStream(asset);
                }
                else {
                    return null;
                }
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
    public void streamAsset(@RequestParam(defaultValue="true", required=false) Boolean fallback, @RequestParam(value="ext", required=false) String ext, @PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Asset asset = assetService.get(id);
        boolean canExport = SecurityUtils.canExport(asset);
        StreamFile format = getPreferredFormat(asset, ext, fallback, !canExport);

        /*
         * Nothing to return...
         */
        if (format == null) {
            response.setStatus(404);
        }
        else {
            try {
                MultipartFileSender.fromPath(Paths.get(format.path))
                        .with(request)
                        .with(response)
                        .setContentType(format.mimeType)
                        .serveResource();
                if (canExport) {
                    logService.logAsync(UserLogSpec.build(LogAction.View, "asset", asset.getId()));
                }
            } catch (Exception e) {
                logger.warn("MultipartFileSender failed on {}, unexpected {}", id, e.getMessage());
            }
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
            if (proxy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
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
            if (proxy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
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
            if (proxy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
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

    @RequestMapping(value="/api/v1/assets/_fields/hide", method=RequestMethod.PUT)
    public Object unhideField(@RequestBody HideField update) throws IOException {
        return HttpUtils.status("field", "hide",
                searchService.updateField(update.setHide(true).setManual(true)));
    }

    @RequestMapping(value="/api/v1/assets/_fields/hide", method=RequestMethod.DELETE)
    public Object hideField(@RequestBody HideField update) throws IOException {
        return HttpUtils.status("field", "unhide",
                searchService.updateField(update.setHide(false)));
    }

    @RequestMapping(value="/api/v1/assets/_mapping", method=RequestMethod.GET)
    public Map<String,Object> getMapping() throws IOException {
        return assetService.getMapping();
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
    public String suggestV2(@RequestBody AssetSuggestBuilder builder) throws IOException {
        SuggestResponse response = searchService.suggest(builder.getText());
        return response.toString();
    }

    @RequestMapping(value="/api/v3/assets/_suggest", method=RequestMethod.POST)
    public Object suggestV3(@RequestBody AssetSuggestBuilder suggest) throws IOException {
        return searchService.getSuggestTerms(suggest.getText());
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

    @RequestMapping(value="/api/v1/assets/{id}/_elements", method=RequestMethod.GET)
    public PagedList<Document> getElements(@PathVariable String id,
                                           @RequestParam(value="from", required=false) Integer from,
                                           @RequestParam(value="count", required=false) Integer count) {
        return assetService.getElements(id, new Pager(from, count));
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
        return HttpUtils.updated("asset", id, true, assetService.get(id));
    }

    @RequestMapping(value="/api/v1/assets/_index", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public AssetIndexResult index(@RequestBody AssetIndexSpec spec) throws IOException {
        return assetService.index(spec);
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
