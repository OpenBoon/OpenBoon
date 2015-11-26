package com.zorroa.archivist.sdk.schema;

/**
 * Created by chambers on 11/25/15.
 */
public class IngestSchema implements Schema {

    private long id;
    private int pipeline;
    private String path;

    @Override
    public String getNamespace() {
        return "ingest";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPipeline() {
        return pipeline;
    }

    public void setPipeline(int pipeline) {
        this.pipeline = pipeline;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
