package com.zorroa.archivist.sdk.processor.export;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.processor.Processor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An ExportProcessor is for defining a self contained piece of business logic
 * for use within a Export Pipeline.
 */
public abstract class ExportProcessor extends Processor {

    private static final AtomicLong NUMBER = new AtomicLong();

    /**
     * Generates a unique name for an ExportProcessor, which gets used to create
     * its working directory.  The purpose of it being a class name with a number
     * is for easy troubleshooting while inspecting data left behind by an export.
     *
     * @param processor
     * @return
     */
    public static String generateName(Class<? extends ExportProcessor> processor) {
        return String.format("%s%04d", processor.getName().substring(
                processor.getName().lastIndexOf(".")+1), NUMBER.incrementAndGet());
    }

    private final String name;
    private String workingDirectory;
    protected Asset asset;

    protected List<Port<?>> inputPorts = Lists.newArrayList();
    protected List<Port<?>> outputPorts = Lists.newArrayList();

    public ExportProcessor() {
        this.name = generateName(this.getClass());
    }

    /**
     * This function is called by the Export processing engine.  It sets the current
     * Asset being worked on, as well as the current working directory.
     *
     * @param asset
     * @param workingDirectory
     * @throws Exception
     */
    public void execute(Asset asset, String workingDirectory) throws Exception {
        this.asset = asset;
        this.workingDirectory = workingDirectory;

        process();
    }

    /**
     * This method must be implemenented by subclasses.  If an exception is thrown
     * here, the current asset is skipped over.
     *
     * @throws Exception
     */
    protected abstract void process() throws Exception;

    public Asset getAsset() {
        return asset;
    }

    public String getName() {
        return name;
    }

    public List<Port<?>> getInputPorts() {
        return inputPorts;
    }

    public List<Port<?>> getOutputPorts() {
        return outputPorts;
    }

    public Port<?> addInputPort(Port<?> port) {
        inputPorts.add(port);
        return port;
    }

    public Port<?> addOutputPort(Port<?> port) {
        outputPorts.add(port);
        return port;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }
}
