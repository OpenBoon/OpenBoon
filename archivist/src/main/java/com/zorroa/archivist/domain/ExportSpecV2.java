package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 7/11/16.
 */
public class ExportSpecV2 {

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
     * A custom pipeline to run the assets through. Can be null.
     */
    public List<ProcessorRef> processors;

    /**
     * Utilize a pre-existing Export pipeline.
     */
    private List<Object> pipelineIds;

    /**
     *
     */
    private boolean source = true;

    /**
     * A list of fields which metadata exporters can default to using.
     */
    private List<String> fields;

    public AssetSearch getSearch() {
        return search;
    }

    public ExportSpecV2 setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public ExportSpecV2 setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
        return this;
    }

    public String getName() {
        return name;
    }

    public ExportSpecV2 setName(String name) {
        this.name = name;
        return this;
    }

    public List<Object> getPipelineIds() {
        return pipelineIds;
    }

    public ExportSpecV2 setPipelineIds(List<Object> pipelineIds) {
        this.pipelineIds = pipelineIds;
        return this;
    }

    public boolean isSource() {
        return source;
    }

    public ExportSpecV2 setSource(boolean source) {
        this.source = source;
        return this;
    }

    public List<String> getFields() {
        return fields;
    }

    public ExportSpecV2 setFields(List<String> fields) {
        this.fields = fields;
        return this;
    }
}
