package com.zorroa.archivist.web;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@RestController
public class FolderController {

    @Autowired
    FolderService folderService;

    @Autowired
    AssetService assetService;

    @Autowired
    SearchService searchService;

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.POST)
    public Folder create(@RequestBody FolderBuilder builder, HttpSession httpSession) {
        return folderService.create(builder);
    }

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.GET)
    public List<Folder> getAll() {
        return folderService.getAll();
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.GET)
    public Folder get(@PathVariable int id) {
        return folderService.get(id);
    }

    @RequestMapping(value="/api/v1/folders/_/**", method=RequestMethod.GET)
    public Folder get(HttpServletRequest request) {
        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring(path.indexOf("/_/") + 2);
        return folderService.get(path);
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.PUT)
    public Folder update(@RequestBody FolderBuilder builder, @PathVariable int id) {
        Folder folder = folderService.get(id);
        folderService.update(folder, builder);
        return folderService.get(folder.getId());
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.DELETE)
    public Folder delete(@PathVariable int id) {
        Folder folder = folderService.get(id);
        if (folderService.delete(folder)) {
            return folder;
        }
        return null;
    }

    @RequestMapping(value="/api/v1/folders/{id}/_children", method=RequestMethod.GET)
    public List<Folder> getChildren(@PathVariable int id) {
        Folder folder = folderService.get(id);
        return folderService.getChildren(folder);
    }

    @RequestMapping(value="/api/v1/folders/{id}/assets", method=RequestMethod.GET)
    public void getAssets(@PathVariable int id, HttpServletResponse httpResponse) throws IOException {
        HttpUtils.writeElasticResponse(searchService.search(
                new AssetSearch().setFilter(new AssetFilter().addToFolderIds(id))), httpResponse);
    }

    /**
     * Remove the given list of asset Ids from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @RequestMapping(value="/api/v1/folders/{id}/assets", method=RequestMethod.DELETE)
    public void removeAsset(
            @RequestBody List<String> assetIds,
            @PathVariable Integer id) throws Exception {
        Folder folder = folderService.get(id);
        for (String assetId: assetIds) {
            Asset asset = assetService.get(assetId);
            assetService.removeFromFolder(asset, folder);
        }
    }

    /**
     * Add a given list of asset Ids from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @RequestMapping(value="/api/v1/folders/{id}/assets", method=RequestMethod.POST)
    public void addAsset(
            @RequestBody List<String> assetIds,
            @PathVariable Integer id) throws Exception {
        Folder folder = folderService.get(id);
        for (String assetId: assetIds) {
            Asset asset = assetService.get(assetId);
            assetService.addToFolder(asset, folder);
        }
    }
}
