package com.zorroa.archivist.rest;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.service.IngestService;

@RestController
public class IngestPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @RequestMapping(value="/pipeline", method=RequestMethod.POST)
    public IngestPipeline create(@RequestBody String builder) throws JsonParseException, JsonMappingException, IOException {
        logger.info("posted: {}", builder);
        return ingestService.createIngestPipeline(Json.Mapper.readValue(builder, IngestPipelineBuilder.class));
    }

    @RequestMapping(value="/pipeline/{id}", method=RequestMethod.GET)
    public IngestPipeline get(@PathVariable String id) {
        return ingestService.getIngestPipeline(id);
    }

    @RequestMapping(value="/pipeline", method=RequestMethod.GET)
    public List<IngestPipeline> getAll() {
        return ingestService.getIngestPipelines();
    }
}
