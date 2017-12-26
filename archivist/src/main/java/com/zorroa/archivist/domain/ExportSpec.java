package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * The ExportSpec describes and export.
 *
 * All export features are driven by pipelines.  If no processors or pipelines are
 * specified, the export system will use the standard export pipeline as defined
 * by the admin.
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
     * A custom pipeline to run the assets through. Can be null.
     */
    public List<ProcessorRef> processors;

    /**
     * Any global args to the script
     */
    public Map<String, Object> args = ImmutableMap.of();

    public AssetSearch getSearch() {
        return search;
    }

    public ExportSpec setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public ExportSpec setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
        return this;
    }

    public String getName() {
        return name;
    }

    public ExportSpec setName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public ExportSpec setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }
}
