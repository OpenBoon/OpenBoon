package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;

import java.util.List;

/**
 * A simplified spec for processing a file import with uploaded files.
 */
public class UploadImportSpec {

    /**
     * An optional name for the export.  If no name is specified
     * a simple name like "import by 'username'" is added.
     */
    private String name;

    /**
     * A custom pipeline to run the assets through. Can be null.
     */
    public List<ProcessorRef> processors;

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public UploadImportSpec setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
        return this;
    }

    public String getName() {
        return name;
    }

    public UploadImportSpec setName(String name) {
        this.name = name;
        return this;
    }
}
