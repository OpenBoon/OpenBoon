package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobFilter;
import com.zorroa.archivist.domain.JobSpecV;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.exception.MalformedDataException;
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

    @RequestMapping(value="/api/v1/jobs/{id}/_cancel", method = RequestMethod.PUT)
    public Object cancel(@PathVariable Integer id) throws IOException {
        return HttpUtils.status("job", id, "cancel", jobService.cancel(() -> id));
    }

    @RequestMapping(value="/api/v1/jobs/{id}/_restart", method = RequestMethod.PUT)
    public Object restart(@PathVariable Integer id) throws IOException {
        return HttpUtils.status("job", id, "restart", jobService.restart(() -> id));
    }

    @RequestMapping(value="/api/v1/jobs/{id}", method = RequestMethod.GET)
    public Object get(@PathVariable int id) throws IOException {
        return jobService.get(id);
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
            throw new MalformedDataException(HttpUtils.getBindingErrorString(valid));
        }
        return jobService.launch(spec);
    }
}
