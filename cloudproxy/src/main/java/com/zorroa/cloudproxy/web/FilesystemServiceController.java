package com.zorroa.cloudproxy.web;

import com.google.common.collect.Lists;
import com.zorroa.cloudproxy.domain.FilesystemEntry;
import com.zorroa.cloudproxy.service.FilesystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by wex on 4/1/17.
 */
@RestController
public class FilesystemServiceController {

    @Autowired
    FilesystemService filesystemService;

    @CrossOrigin(origins = "http://localhost:8080")
    @RequestMapping(value="/api/v1/files/_path", method = RequestMethod.PUT)
    public List<FilesystemEntry> get(@RequestBody Map<String,String> path) {
        return filesystemService.get(path.get("path"), Lists.newArrayList("/etc", "/usr/bin", "/usr/lib"));
    }
}
