package com.zorroa.archivist.sdk.schema;

/**
 * The ingest schema contains all the information related to the ingest that brought in the asset.
 */
public class IngestSchema implements Schema {

    private int id;
    private int pipeline;
    private String path;

    @Override
    public String getNamespace() {
        return "ingest";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
