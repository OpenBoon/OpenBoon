package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestFilter;
import com.zorroa.archivist.domain.IngestUpdateBuilder;
import com.zorroa.archivist.service.IngestSchedulerService;
import com.zorroa.archivist.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class IngestController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.POST)
    public Ingest create(@RequestBody IngestBuilder builder) {
        return ingestService.createIngest(builder);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.GET)
    public Ingest get(@PathVariable String id) {
        return ingestService.getIngest(Long.valueOf(id));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.GET)
    public List<Ingest> getAll() {
        return ingestService.getAllIngests();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests/_search", method=RequestMethod.POST)
    public List<Ingest> search(@RequestBody IngestFilter filter) {
        return ingestService.getIngests(filter);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests/{id}/_execute", method=RequestMethod.POST)
    public Ingest ingest(@PathVariable String id) {
        Ingest ingest = ingestService.getIngest(Long.valueOf(id));
        ingestSchedulerService.executeIngest(ingest);
        return ingest;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.PUT)
    public Ingest update(@RequestBody IngestUpdateBuilder builder, @PathVariable String id) {
        Ingest ingest = ingestService.getIngest(Long.valueOf(id));
        ingestService.updateIngest(ingest, builder);
        return ingestService.getIngest(ingest.getId());
    }
}
