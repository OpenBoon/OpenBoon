package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.domain.*;
import com.zorroa.archivist.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class IngestPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.POST)
    public IngestPipeline create(@RequestBody IngestPipelineBuilder builder) {
        return ingestService.createIngestPipeline(builder);
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.GET)
    public IngestPipeline get(@PathVariable Integer id) {
        return ingestService.getIngestPipeline(id);
    }

    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.GET)
    public List<IngestPipeline> getAll() {
        return ingestService.getIngestPipelines();
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines/{id}/_ingest", method=RequestMethod.POST)
    public Ingest ingest(@RequestBody IngestBuilder builder, @PathVariable Integer id) {
        return ingestService.createIngest(builder);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.PUT)
    public IngestPipeline update(@RequestBody IngestPipelineUpdateBuilder builder, @PathVariable Integer id) {
        IngestPipeline pipeline = ingestService.getIngestPipeline(id);
        ingestService.updateIngestPipeline(pipeline, builder);
        return ingestService.getIngestPipeline(id);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.DELETE)
    public Map<String, Object> delete(@PathVariable Integer id) {
        IngestPipeline pipeline = ingestService.getIngestPipeline(id);
        try {
            return ImmutableMap.<String, Object>builder()
                    .put("status", ingestService.deleteIngestPipeline(pipeline))
                    .build();
        } catch (Exception e) {
            return ImmutableMap.<String, Object>builder()
                    .put("status", false)
                    .put("message", e.getMessage())
                    .build();
        }
    }
}
