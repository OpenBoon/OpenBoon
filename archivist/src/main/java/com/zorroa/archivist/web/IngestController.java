package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
@RestController
public class IngestController {

    private static final Logger logger = LoggerFactory.getLogger(IngestPipelineController.class);

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.POST)
    public Ingest create(@RequestBody IngestBuilder builder) {
        return ingestService.createIngest(builder);
    }

    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.GET)
    public Ingest get(@PathVariable int id) {
        return ingestService.getIngest(id);
    }

    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.GET)
    public List<Ingest> getAll() {
        return ingestService.getAllIngests();
    }

    @RequestMapping(value="/api/v1/ingests/_search", method=RequestMethod.POST)
    public List<Ingest> search(@RequestBody IngestFilter filter) {
        return ingestService.getIngests(filter);
    }

    @RequestMapping(value="/api/v1/ingests/{id}/_execute", method=RequestMethod.POST)
    public Ingest ingest(@PathVariable int id) {
        Ingest ingest = ingestService.getIngest(id);
        ingestExecutorService.executeIngest(ingest);
        return ingest;
    }

    @RequestMapping(value="/api/v1/ingests/{id}/_pause", method=RequestMethod.PUT)
    public Ingest pause(@PathVariable int id) {
        Ingest ingest = ingestService.getIngest(id);
        ingestExecutorService.pause(ingest);
        return ingest;
    }

    @RequestMapping(value="/api/v1/ingests/{id}/_stop", method=RequestMethod.PUT)
    public Ingest stop(@PathVariable int id) {
        Ingest ingest = ingestService.getIngest(id);
        ingestExecutorService.stop(ingest);
        return ingest;
    }

    @RequestMapping(value="/api/v1/ingests/{id}/_resume", method=RequestMethod.PUT)
    public Ingest resume(@PathVariable int id) {
        Ingest ingest = ingestService.getIngest(id);
        ingestExecutorService.resume(ingest);
        return ingest;
    }

    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.PUT)
    public Ingest update(@RequestBody IngestUpdateBuilder builder, @PathVariable int id) {
        Ingest ingest = ingestService.getIngest(id);
        ingestService.updateIngest(ingest, builder);
        return ingestService.getIngest(ingest.getId());
    }

    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.DELETE)
    public Map<String, Object> delete(@PathVariable int id) {
        Ingest ingest = ingestService.getIngest(id);

        try {

            if (!ingest.getState().equals(IngestState.Idle)) {
                throw new IllegalStateException("Ingest must be idle to be deleted.");
            }
            return ImmutableMap.<String, Object>builder()
                    .put("status", ingestService.deleteIngest(ingest))
                    .build();

        } catch (Exception e) {
            return ImmutableMap.<String, Object>builder()
                    .put("false", ingestService.deleteIngest(ingest))
                    .put("message", e.getMessage())
                    .build();
        }
    }
}
