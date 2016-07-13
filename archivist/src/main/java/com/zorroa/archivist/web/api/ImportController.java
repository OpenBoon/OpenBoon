package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.service.ImportService;
import com.zorroa.archivist.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by chambers on 7/11/16.
 */
@RestController
public class ImportController {

    @Autowired
    ImportService importService;

    @Autowired
    JobService jobService;

    @RequestMapping(value="/api/v1/imports", method= RequestMethod.POST)
    public Object create(@RequestBody ImportSpec spec) throws IOException {
        return importService.create(spec);
    }

    @RequestMapping(value="/api/v1/imports/{id}", method= RequestMethod.POST)
    public Object get(@PathVariable Integer id) throws IOException {
        return jobService.get(id);
    }
}
