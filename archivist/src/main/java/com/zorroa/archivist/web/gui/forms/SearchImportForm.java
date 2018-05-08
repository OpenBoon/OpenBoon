package com.zorroa.archivist.web.gui.forms;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Created by chambers on 10/3/16.
 */
public class SearchImportForm {

    @NotEmpty
    private String search;

    @NotNull
    private UUID pipelineId;

    public String getSearch() {
        return search;
    }

    public SearchImportForm setSearch(String search) {
        this.search = search;
        return this;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public SearchImportForm setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }
}
