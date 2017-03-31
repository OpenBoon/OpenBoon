package com.zorroa.archivist.domain;

import org.springframework.web.multipart.MultipartFile;

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
    public Integer pipelineId;

    /**
     * An option array of assets, uploaded with the form.  These have to be written
     * someplace, probably to the OFS for processing but maybe we allow them to be
     * written into a given path in OFS.
     */
    private List<MultipartFile> files;

    public Integer getPipelineId() {
        return pipelineId;
    }

    public UploadImportSpec setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public String getName() {
        return name;
    }

    public UploadImportSpec setName(String name) {
        this.name = name;
        return this;
    }

    public List<MultipartFile> getFiles() {
        return files;
    }

    public UploadImportSpec setFiles(List<MultipartFile> files) {
        this.files = files;
        return this;
    }
}
