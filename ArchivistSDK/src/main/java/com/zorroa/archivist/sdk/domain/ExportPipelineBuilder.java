package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.processor.ExportProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public class ExportPipelineBuilder {

    private String name;
    private List<ExportProcessorFactory<ExportProcessor>> processors = Lists.newArrayList();
    private List<ConnectionBuilder> connections = Lists.newArrayList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ExportProcessorFactory<ExportProcessor>> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ExportProcessorFactory<ExportProcessor>> processors) {
        this.processors = processors;
    }

    public void addToProcessors(ExportProcessorFactory<ExportProcessor> processor) {
        processors.add(processor);
    }

    public void addToConnections(ConnectionBuilder builder) {
        connections.add(builder);
    }

    public void addToConnections(String cord, String socket) {
        connections.add(new ConnectionBuilder(cord, socket));
    }

    public void addToConnections(ExportProcessorFactory cord, String cordPort, ExportProcessorFactory socket, String socketPort) {
        connections.add(new ConnectionBuilder(
                String.format("%s::%s", cord.getName(), cordPort),
                String.format("%s::%s", socket.getName(), socketPort)));
    }

    public List<ConnectionBuilder> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionBuilder> connections) {
        this.connections = connections;
    }


    @Override
    public String toString() {
        return String.format("<ExportPipelineBuilder name='%s'>", name);
    }

}
