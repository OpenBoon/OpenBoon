package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorSpec;
import com.zorroa.sdk.search.AssetSearch;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 7/11/16.
 */
public class ExportSpec {

    @NotEmpty
    private AssetSearch search;

    /**
     * A custom pipeline to run the assets through. Can be null.
     */
    protected List<ProcessorSpec> pipeline;

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

    public List<ProcessorSpec> getPipeline() {
        return pipeline;
    }

    public ExportSpec setPipeline(List<ProcessorSpec> pipeline) {
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
}
