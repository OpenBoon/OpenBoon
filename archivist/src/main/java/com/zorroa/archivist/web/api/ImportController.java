package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.DebugImportSpec;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.service.ImportService;
import com.zorroa.archivist.service.JobExecutorService;
import com.zorroa.archivist.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    JobExecutorService jobExecutorService;

    @RequestMapping(value="/api/v1/imports", method = RequestMethod.POST)
    public Object create(@RequestBody ImportSpec spec) throws IOException {
        Job job = importService.create(spec);
        //jobExecutorService.queueSchedule();
        return job;
    }


    @PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/imports/_debug", method = RequestMethod.POST)
    public Object create_debug(@RequestBody DebugImportSpec spec) throws IOException, InterruptedException {
        Job job = importService.create(spec);
        return jobExecutorService.waitOnResponse(job);
    }

    @RequestMapping(value="/api/v1/imports/{id}", method = RequestMethod.GET)
    public Object get(@PathVariable Integer id) throws IOException {
        return jobService.get(id);
    }
}
