package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableList;
import com.zorroa.sdk.processor.ProcessorSpec;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

public class PipelineSpec {

    @NotNull
    private List<ProcessorSpec> processors = ImmutableList.of();

    @NotNull
    private PipelineType type;

    @NotEmpty
    @Pattern(regexp="^[a-z].*$", flags={Pattern.Flag.CASE_INSENSITIVE})
    private String name;

    public List<ProcessorSpec> getProcessors() {
        return processors;
    }

    public PipelineSpec setProcessors(List<ProcessorSpec> processors) {
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
}
