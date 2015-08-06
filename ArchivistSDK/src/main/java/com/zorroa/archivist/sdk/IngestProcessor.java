package com.zorroa.archivist.sdk;

import java.util.Map;

public abstract class IngestProcessor {

    protected IngestProcessorService ingestProcessorService;

    public IngestProcessorService getIngestProcessorService() {
        return ingestProcessorService;
    }

    public void setIngestProcessorService(IngestProcessorService ingestProcessorService) {
        this.ingestProcessorService = ingestProcessorService;
    }

    public abstract void process(AssetBuilder asset);

    private Map<String, Object> args;

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public void teardown() { /* Cleanup ThreadLocal or other per-thread values */ }
}
