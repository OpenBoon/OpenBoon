package com.zorroa.archivist.web;

import java.util.List;

import com.zorroa.archivist.service.IngestSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.service.IngestService;

@RestController
public class IngestController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.POST)
    public Ingest ingest(@RequestBody IngestBuilder builder) {
        return ingestService.createIngest(builder);
    }

    /**
     * Returns just waiting and active ingests.
     *
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.GET)
    public List<Ingest> getPending() {
        return ingestService.getPendingIngests();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.GET)
    public Ingest get(@PathVariable String id) {
        return ingestService.getIngest(Long.valueOf(id));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/ingests/{id}/_ingest", method=RequestMethod.POST)
    public Ingest ingest(@PathVariable String id) {
        Ingest ingest = ingestService.getIngest(Long.valueOf(id));
        ingestSchedulerService.executeIngest(ingest);
        return ingest;
    }

}
