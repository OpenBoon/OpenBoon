package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.UploadImportSpec;
import com.zorroa.archivist.service.ImportService;
import com.zorroa.archivist.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Created by chambers on 7/11/16.
 */
@RestController
public class ImportController {

    @Autowired
    ImportService importService;

    @Autowired
    JobService jobService;

    /**
     *
     * @param spec
     * @return
     * @throws IOException
     */
    @RequestMapping(value="/api/v1/imports/_upload", method = RequestMethod.POST)
    public Object upload(UploadImportSpec spec) throws IOException {
        Job job = importService.create(spec);
        return job;
    }

    @PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/imports/_suggest", method = RequestMethod.GET)
    public Object suggest(@RequestBody Map<String,String> body) throws IOException {
        return importService.suggestImportPath(body.get("path"));
    }

    @RequestMapping(value="/api/v1/imports", method = RequestMethod.POST)
    public Object create(@RequestBody ImportSpec spec) throws IOException {
        Job job = importService.create(spec);
        return job;
    }

    @RequestMapping(value="/api/v1/imports/{id}", method = RequestMethod.GET)
    public Object get(@PathVariable Integer id) throws IOException {
        return jobService.get(id);
    }
}
