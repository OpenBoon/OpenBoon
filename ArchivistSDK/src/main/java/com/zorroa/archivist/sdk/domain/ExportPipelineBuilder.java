package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public class ExportPipelineBuilder {

    private String name;
    private List<ProcessorFactory<ExportProcessor>> processors = Lists.newArrayList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProcessorFactory<ExportProcessor>> getProcessors() {
        return processors;
    }

    public void addToProcessors(ProcessorFactory<ExportProcessor> processor) {
        processors.add(processor);
    }

    @Override
    public String toString() {
        return String.format("<ExportPipelineBuilder name='%s'>", name);
    }

}
