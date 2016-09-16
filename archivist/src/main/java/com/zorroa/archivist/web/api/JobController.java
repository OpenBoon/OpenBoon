package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Unified controller for manipulating jobs of any type (import, export, etc)
 */
@RestController
public class JobController {

    @Autowired
    JobService jobService;

    @RequestMapping(value="/api/v1/jobs/{id}/_cancel", method = RequestMethod.PUT)
    public Object cancel(@PathVariable Integer id) throws IOException {
        return HttpUtils.status("job", id, "cancel", jobService.cancel(() -> id));
    }

    @RequestMapping(value="/api/v1/jobs/{id}/_restart", method = RequestMethod.PUT)
    public Object restart(@PathVariable Integer id) throws IOException {
        return HttpUtils.status("job", id, "restart", jobService.restart(() -> id));
    }
}
