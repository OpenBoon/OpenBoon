package com.zorroa.archivist.ingest;

import java.io.File;
import java.util.Map;

import com.zorroa.archivist.domain.AssetBuilder;

public abstract class IngestProcessor {

    public abstract void process(AssetBuilder builder, File stream);

    private Map<String, Object> args;

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}
