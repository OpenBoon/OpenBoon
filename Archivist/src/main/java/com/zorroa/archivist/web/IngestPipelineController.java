package com.zorroa.archivist.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.service.IngestService;

@RestController
public class IngestPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value="/pipelines", method=RequestMethod.POST)
    public IngestPipeline create(@RequestBody IngestPipelineBuilder builder) {
        return ingestService.createIngestPipeline(builder);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value="/pipelines/{id}", method=RequestMethod.GET)
    public IngestPipeline get(@PathVariable String id) {
        return ingestService.getIngestPipeline(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value="/pipelines", method=RequestMethod.GET)
    public List<IngestPipeline> getAll() {
        return ingestService.getIngestPipelines();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value="/pipelines/{id}/_ingest", method=RequestMethod.POST)
    public void ingest(@RequestBody IngestBuilder builder, @PathVariable String id) {
        IngestPipeline pipeline = ingestService.getIngestPipeline(id);
        ingestService.ingest(pipeline, builder);
    }
}
