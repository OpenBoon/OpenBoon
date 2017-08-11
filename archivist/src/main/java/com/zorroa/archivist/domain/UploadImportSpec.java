package com.zorroa.archivist.domain;

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
     * Utilize a pre-existing pipeline.
     */
    public List<Object> pipelineIds;

    public List<Object> getPipelineIds() {
        return pipelineIds;
    }

    public UploadImportSpec setPipelineIds(List<Object> pipelineIds) {
        this.pipelineIds = pipelineIds;
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
