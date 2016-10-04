package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableList;
import com.zorroa.sdk.processor.ProcessorRef;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * A validated version of the PipelineSpec which is submitted via a REST endpoint.
 */
public class PipelineSpecV {

    @NotNull
    private List<ProcessorRef> processors = ImmutableList.of();

    @NotNull
    private PipelineType type;

    @NotEmpty
    @Pattern(regexp="^[a-z].*$", flags={Pattern.Flag.CASE_INSENSITIVE})
    private String name;

    @NotEmpty
    private String description;

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public PipelineSpecV setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public PipelineSpecV setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public PipelineSpecV setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PipelineSpecV setDescription(String description) {
        this.description = description;
        return this;
    }
}
