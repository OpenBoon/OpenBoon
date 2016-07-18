package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableList;
import com.zorroa.sdk.plugins.ModuleRef;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

public class PipelineSpec {

    @NotNull
    private List<ModuleRef> processors = ImmutableList.of();

    @NotNull
    private PipelineType type;

    @NotEmpty
    @Pattern(regexp="^[a-z].*$", flags={Pattern.Flag.CASE_INSENSITIVE})
    private String name;

    @NotEmpty
    private String description;

    public List<ModuleRef> getProcessors() {
        return processors;
    }

    public PipelineSpec setProcessors(List<ModuleRef> processors) {
        this.processors = processors;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public PipelineSpec setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public PipelineSpec setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PipelineSpec setDescription(String description) {
        this.description = description;
        return this;
    }
}
