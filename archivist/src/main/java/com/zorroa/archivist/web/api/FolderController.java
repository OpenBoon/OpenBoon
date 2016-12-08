package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.service.AssetService;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.MessagingService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.sdk.search.AssetSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class FolderController {

    @Autowired
    FolderService folderService;

    @Autowired
    AssetService assetService;

    @Autowired
    SearchService searchService;

    @Autowired
    MessagingService messagingService;

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.POST)
    public Folder create(@RequestBody FolderSpec spec) {
        return folderService.create(spec, false);
    }

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.GET)
    public List<Folder> getAll() {
        return folderService.getAll();
    }

    @RequestMapping(value="/api/v1/folders/{id:\\d+}", method=RequestMethod.GET)
    public Folder get(@PathVariable int id) {
        return folderService.get(id);
    }

    @RequestMapping(value="/api/v1/folders/{id}/_assetCount", method=RequestMethod.GET)
    public Object countAssets(@PathVariable int id) {
        return HttpUtils.count(searchService.count(folderService.get(id)));
    }

    public static class FolderAssetCountsRequest {
        public List<Integer> ids;
        public AssetSearch search;
    }

    @RequestMapping(value="/api/v1/folders/_assetCounts", method=RequestMethod.POST)
    public Object countAssets(@RequestBody FolderAssetCountsRequest req) {
        return HttpUtils.counts(searchService.count(req.ids, req.search));
    }

    @RequestMapping(value="/api/v1/folders/_path/**", method=RequestMethod.GET)
    public Folder get(HttpServletRequest request) {
        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring(path.indexOf("/_path/") + 6).replace("//", "/");
        Folder f =  folderService.get(path);
        return f;
    }

    @Deprecated
    @RequestMapping(value="/api/v1/folders/_exists/**", method=RequestMethod.GET)
    public Object exists(HttpServletRequest request) {
        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.replace("/api/v1/folders/_exists", "");
        return HttpUtils.status("folders", path, "exists", folderService.exists(path));
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.PUT)
    public Folder update(@RequestBody Folder folder, @PathVariable int id) {
        folderService.update(id, folder);
        return folderService.get(folder.getId());
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.DELETE)
    public Object delete(@PathVariable int id) {
        Folder folder = folderService.get(id);
        return HttpUtils.deleted("folders", id, folderService.trash(folder).getCount() > 0);
    }

    @RequestMapping(value="/api/v1/folders/{id}/folders", method=RequestMethod.GET)
    public List<Folder> getChildFolders(@PathVariable int id) {
        Folder folder = folderService.get(id);
        return folderService.getChildren(folder);
    }

    @Deprecated
    @RequestMapping(value="/api/v1/folders/{id}/_children", method=RequestMethod.GET)
    public List<Folder> getChildren(@PathVariable int id) {
        Folder folder = folderService.get(id);
        return folderService.getChildren(folder);
    }

    @RequestMapping(value="/api/v1/folders/{id}/folders/{name}", method=RequestMethod.GET)
    public Folder getChild(@PathVariable int id, @PathVariable String name) {
        return folderService.get(id, name);
    }

    /**
     * Remove the given list of asset Ids from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @RequestMapping(value="/api/v1/folders/{id}/assets", method=RequestMethod.DELETE)
    public Object removeAssets(
            @RequestBody List<String> assetIds,
            @PathVariable Integer id) throws Exception {
        Folder folder = folderService.get(id);
        return folderService.removeAssets(folder, assetIds);
    }

    /**
     * Add a given list of asset Ids from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @RequestMapping(value="/api/v1/folders/{id}/assets", method=RequestMethod.POST)
    public Object addAssets(
            @RequestBody List<String> assetIds,
            @PathVariable Integer id) throws Exception {
        Folder folder = folderService.get(id);
        return folderService.addAssets(folder, assetIds);
    }
}
