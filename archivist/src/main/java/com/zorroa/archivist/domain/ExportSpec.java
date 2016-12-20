package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 7/11/16.
 */
public class ExportSpec {

    /**
     * An optional name for the export.
     */
    private String name;

    /**
     * The search for the assets to include in the export.
     */
    @NotEmpty
    private AssetSearch search;

    /**
     * An optional list of fields to export with the data into a CSV file.
     */
    private List<String> fields;

    /**
     * A custom pipeline to run the assets through. Can be null.
     */
    protected List<ProcessorRef> pipeline;

    /**
     * Utilize a pre-existing import pipeline.
     */
    protected Integer pipelineId;


    public AssetSearch getSearch() {
        return search;
    }

    public ExportSpec setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public List<ProcessorRef> getPipeline() {
        return pipeline;
    }

    public ExportSpec setPipeline(List<ProcessorRef> pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public ExportSpec setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public String getName() {
        return name;
    }

    public ExportSpec setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getFields() {
        return fields;
    }

    public ExportSpec setFields(List<String> fields) {
        this.fields = fields;
        return this;
    }
}
