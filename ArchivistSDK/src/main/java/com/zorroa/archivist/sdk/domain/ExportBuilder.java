package com.zorroa.archivist.sdk.domain;


import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public class ExportBuilder {

    /**
     * Defines the list of assets to export.
     */
    private AssetSearchBuilder search;

    /**
     * Defines the options for the export.
     */
    private ExportOptions options;

    /**
     * A note or description of the export.
     */
    private String note;

    /**
     * Defines the outputs that should be generated.
     */
    private List<ProcessorFactory<ExportProcessor>> outputs;

    public ExportBuilder() {}

    public AssetSearchBuilder getSearch() {
        return search;
    }

    public void setSearch(AssetSearchBuilder search) {
        this.search = search;
    }

    public ExportOptions getOptions() {
        return options;
    }

    public void setOptions(ExportOptions options) {
        this.options = options;
    }

    public  List<ProcessorFactory<ExportProcessor>> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ProcessorFactory<ExportProcessor>> outputs) {
        this.outputs = outputs;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
