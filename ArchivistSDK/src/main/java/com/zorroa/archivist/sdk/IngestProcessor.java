package com.zorroa.archivist.sdk;

import java.util.Map;

public abstract class IngestProcessor {

    static public IngestService ingestService = null;

    public abstract void process(AssetBuilder asset);

    private Map<String, Object> args;

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}
