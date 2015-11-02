package com.zorroa.archivist.sdk.processor.ingest;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.Processor;
import com.zorroa.archivist.sdk.service.IngestProcessorService;

import java.util.Map;

public abstract class IngestProcessor extends Processor {

    public IngestProcessor() { };

    public abstract void process(AssetBuilder asset);

    /**
     * Runs once after an ingest is completed to clean up any per
     * thread resources.
     */
    public void teardown() { }
}
