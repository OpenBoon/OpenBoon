package com.zorroa.archivist.web;

import java.util.List;

import com.zorroa.archivist.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zorroa.archivist.service.IngestService;

@RestController
public class IngestPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.POST)
    public IngestPipeline create(@RequestBody IngestPipelineBuilder builder) {
        return ingestService.createIngestPipeline(builder);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.GET)
    public IngestPipeline get(@PathVariable String id) {
        return ingestService.getIngestPipeline(id);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.GET)
    public List<IngestPipeline> getAll() {
        return ingestService.getIngestPipelines();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/pipelines/{id}/_ingest", method=RequestMethod.POST)
    public Ingest ingest(@RequestBody IngestBuilder builder, @PathVariable String id) {
        return ingestService.createIngest(builder);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.PUT)
    public IngestPipeline update(@RequestBody IngestPipelineUpdateBuilder builder, @PathVariable Integer id) {
        IngestPipeline pipeline = ingestService.getIngestPipeline(id);
        ingestService.updateIngestPipeline(pipeline, builder);
        return ingestService.getIngestPipeline(id);
    }
}
