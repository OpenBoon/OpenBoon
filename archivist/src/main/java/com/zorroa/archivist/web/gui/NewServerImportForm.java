package com.zorroa.archivist.web.gui;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by chambers on 7/27/16.
 */
public class NewServerImportForm {

    @NotEmpty
    private List<String> paths;

    @NotNull
    private Integer pipelineId;

    public List<String> getPaths() {
        return paths;
    }

    public NewServerImportForm setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public NewServerImportForm setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }
}
