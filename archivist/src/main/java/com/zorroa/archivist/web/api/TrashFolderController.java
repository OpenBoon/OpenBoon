package com.zorroa.archivist.web.api;

import com.google.common.collect.Lists;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.TrashedFolder;
import com.zorroa.archivist.domain.TrashedFolderOp;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.FolderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
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

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

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
     * Restore a list of trash folder IDs.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/trash/_restore", method=RequestMethod.POST)
    public Object restore(@RequestBody List<Integer> ids) {
        List<TrashedFolderOp> restoreOps = Lists.newArrayList();
        for (int id: ids) {
            try {
                TrashedFolder folder = folderService.getTrashedFolder(id);
                TrashedFolderOp op = folderService.restore(folder);
                restoreOps.add(op);
            } catch (Exception e) {
                logger.warn("Failed to restore trash folder: {}", e);
            }
        }
        return HttpUtils.updated("TrashedFolder", ids, restoreOps.size() > 0, restoreOps);
    }

    @RequestMapping(value="/api/v1/trash", method=RequestMethod.DELETE)
    public Object empty(@RequestBody(required = false) List<Integer> ids) {
        if (ids == null) {
            List<Integer> result = folderService.emptyTrash();
            return HttpUtils.deleted("TrashedFolder", result, !result.isEmpty());
        }
        else {
            List<Integer> result = folderService.emptyTrash(ids);
            return HttpUtils.deleted("TrashedFolder", result, !result.isEmpty());
        }

    }

    @RequestMapping(value="/api/v1/trash/_count", method=RequestMethod.GET)
    public Object count() {
        return HttpUtils.count(folderService.trashCount());
    }
}
