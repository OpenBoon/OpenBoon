package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobFilter;
import com.zorroa.archivist.domain.JobSpecV;
import com.zorroa.archivist.service.JobExecutorService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;

/**
 * Unified controller for manipulating jobs of any type (import, export, etc)
 */
@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
public class JobController {

    @Autowired
    JobService jobService;

    @Autowired
    JobExecutorService jobExecutorService;

    @RequestMapping(value="/api/v1/jobs/{id}/_cancel", method = RequestMethod.PUT)
    public Object cancel(@PathVariable Integer id) throws IOException {
        return HttpUtils.status("job", id, "cancel", jobExecutorService.cancelJob(() -> id));
    }

    @RequestMapping(value="/api/v1/jobs/{id}/_restart", method = RequestMethod.PUT)
    public Object restart(@PathVariable Integer id) throws IOException {
        return HttpUtils.status("job", id, "restart", jobExecutorService.restartJob(() -> id));
    }

    @RequestMapping(value="/api/v1/jobs/{id}", method = RequestMethod.GET)
    public Object get(@PathVariable int id) throws IOException {
        return jobService.get(id);
    }

    @RequestMapping(value="/api/v1/jobs/{id}/tasks", method = RequestMethod.GET)
    public Object getTasks(@PathVariable int id,
                           @RequestParam(value="from", required=false) Integer from,
                           @RequestParam(value="count", required=false) Integer count) throws IOException {
        return jobService.getAllTasks(id, new Pager(from, count));
    }

    @RequestMapping(value="/api/v1/jobs", method = RequestMethod.GET)
    public PagedList<Job> getAll(@RequestBody(required = false) JobFilter filter,
                                 @RequestParam(value="from", required=false) Integer from,
                                 @RequestParam(value="count", required=false) Integer count) throws IOException {
        return jobService.getAll(new Pager(from, count), filter);
    }

    @RequestMapping(value="/api/v1/jobs", method = RequestMethod.POST)
    public Object launch(@Valid @RequestBody JobSpecV spec, BindingResult valid) throws IOException {
        if (valid.hasErrors()) {
            throw new ArchivistWriteException(HttpUtils.getBindingErrorString(valid));
        }
        return jobService.launch(spec);
    }
}
