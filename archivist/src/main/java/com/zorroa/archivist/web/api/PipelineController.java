package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.service.PipelineService;
import com.zorroa.sdk.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PipelineController {

    private static final Logger logger = LoggerFactory.getLogger(PipelineController.class);

    @Autowired
    PipelineService pipelineService;

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.POST)
    public IngestPipeline create(@RequestBody IngestPipelineBuilder builder) {
        return pipelineService.createIngestPipeline(builder);
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.GET)
    public IngestPipeline get(@PathVariable Integer id) {
        return pipelineService.getIngestPipeline(id);
    }

    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.GET)
    public List<IngestPipeline> getAll() {
        return pipelineService.getIngestPipelines();
    }

    @RequestMapping(value="/api/v1/pipelines/_by_name/{name}", method=RequestMethod.GET)
    public IngestPipeline get(@PathVariable String name) {
        return pipelineService.getIngestPipeline(name);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.PUT)
    public IngestPipeline update(@RequestBody IngestPipelineUpdateBuilder builder, @PathVariable Integer id) {
        IngestPipeline pipeline = pipelineService.getIngestPipeline(id);
        pipelineService.updateIngestPipeline(pipeline, builder);
        return pipelineService.getIngestPipeline(id);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.DELETE)
    public Map<String, Object> delete(@PathVariable Integer id) {
        IngestPipeline pipeline = pipelineService.getIngestPipeline(id);
        try {
            return ImmutableMap.<String, Object>builder()
                    .put("status", pipelineService.deleteIngestPipeline(pipeline))
                    .build();
        } catch (Exception e) {
            return ImmutableMap.<String, Object>builder()
                    .put("status", false)
                    .put("message", e.getMessage())
                    .build();
        }
    }
}
