package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.service.DyHierarchyService;
import com.zorroa.archivist.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 8/10/16.
 */
@RestController
public class DyHierarchyController {

    @Autowired
    FolderService folderService;

    @Autowired
    DyHierarchyService dyHierarchyService;

    @RequestMapping(value="/api/v1/dyhi/_folder/{id}", method= RequestMethod.GET)
    public DyHierarchy getByFolder(@PathVariable int id) {
        Folder f = folderService.get(id);
        return dyHierarchyService.get(f);
    }

    @RequestMapping(value="/api/v1/dyhi/{id}", method= RequestMethod.GET)
    public DyHierarchy get(@PathVariable int id) {
        return dyHierarchyService.get(id);
    }
}
