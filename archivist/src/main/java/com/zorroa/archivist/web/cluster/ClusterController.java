package com.zorroa.archivist.web.cluster;

import com.zorroa.archivist.service.IngestService;
import com.zorroa.sdk.domain.AnalyzeResult;
import com.zorroa.sdk.domain.Ingest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for Anayst to Archivist communication.
 */
@RestController
public class ClusterController {


    @Autowired
    IngestService ingestService;

    /**
     * Process an analyze result.  Not 100% sure if its the best place to expose this.
     *
     * @param id
     * @param result
     *
     */
    @RequestMapping(value="/cluster/v1/ingest/{id}/_asyncAnalyzeFinished", method= RequestMethod.POST)
    public void batchComplete(@PathVariable int id, @RequestBody AnalyzeResult result) {
        Ingest ingest = ingestService.getIngest(id);
        ingestService.incrementIngestCounters(ingest,
                result.created, result.updated, result.warnings, result.errors);
    }
}
