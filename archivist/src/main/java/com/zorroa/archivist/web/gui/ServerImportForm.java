package com.zorroa.archivist.web.gui;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Created by chambers on 7/27/16.
 */
public class ServerImportForm {

    @NotEmpty
    private String name;

    @NotEmpty
    private List<String> paths;

    @NotNull
    private UUID pipelineId;

    public List<String> getPaths() {
        return paths;
    }

    public ServerImportForm setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public ServerImportForm setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public String getName() {
        return name;
    }

    public ServerImportForm setName(String name) {
        this.name = name;
        return this;
    }
}
