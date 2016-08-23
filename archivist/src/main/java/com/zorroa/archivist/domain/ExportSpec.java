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
     *
     */
    @NotEmpty
    private AssetSearch search;

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
}
