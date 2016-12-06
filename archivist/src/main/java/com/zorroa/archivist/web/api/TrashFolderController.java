package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.TrashedFolder;
import com.zorroa.archivist.domain.TrashedFolderOp;
import com.zorroa.archivist.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TrashFolderController exposes endpoints for controlling a user's trash can.  All
 * of these methods have the current user baked in.
 */
@RestController
public class TrashFolderController {

    @Autowired
    FolderService folderService;

    /**
     * Return a list of Trashed folders for a given user.  This will only
     * return the root level of a series of deleted folders.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/trash", method=RequestMethod.GET)
    public List<TrashedFolder> getAll() {
        return folderService.getTrashedFolders();
    }

    /**
     * Restore a trashed folder to a real folder.
     *
     * @param id
     * @return
     */
    @RequestMapping(value="/api/v1/trash/{id}/_restore", method=RequestMethod.POST)
    public Object get(@PathVariable int id) {
        TrashedFolder folder = folderService.getTrashedFolder(id);
        TrashedFolderOp op = folderService.restore(folder);
        return HttpUtils.updated("TrashedFolder", id, op.getCount() > 0, op);
    }

    @RequestMapping(value="/api/v1/trash/_empty", method=RequestMethod.DELETE)
    public Object empty() {
        int result = folderService.emptyTrash();
        return ImmutableMap.of("type", "TrashedFolder", "success", result >0, "count", result);
    }

    @RequestMapping(value="/api/v1/trash/_count", method=RequestMethod.GET)
    public Object count() {
        return HttpUtils.count(folderService.trashCount());
    }
}
