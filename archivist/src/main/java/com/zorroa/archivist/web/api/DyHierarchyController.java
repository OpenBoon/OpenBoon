package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.DyHierarchySpec;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.service.DyHierarchyService;
import com.zorroa.archivist.service.FolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Created by chambers on 8/10/16.
 */
@RestController
public class DyHierarchyController {

    private static final Logger logger = LoggerFactory.getLogger(DyHierarchyController.class);

    @Autowired
    FolderService folderService;

    @Autowired
    DyHierarchyService dyHierarchyService;

    @RequestMapping(value="/api/v1/dyhi/_folder/{id}", method= RequestMethod.GET)
    public DyHierarchy getByFolder(@PathVariable int id) {
        Folder f = folderService.get(id);
        return dyHierarchyService.get(f);
    }

    @RequestMapping(value="/api/v1/dyhi", method= RequestMethod.POST)
    public DyHierarchy create(@RequestBody DyHierarchySpec spec) {
        return dyHierarchyService.create(spec);
    }

    @RequestMapping(value="/api/v1/dyhi/{id}", method= RequestMethod.POST)
    public Map<String, Object> delete(@PathVariable int id) {
        DyHierarchy dh = dyHierarchyService.get(id);
        boolean result = dyHierarchyService.delete(dh);
        return HttpUtils.status("DyHierarchy", id, "delete", result);
    }

    @RequestMapping(value="/api/v1/dyhi/{id}", method= RequestMethod.GET)
    public DyHierarchy get(@PathVariable int id) {
        return dyHierarchyService.get(id);
    }
}
