package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.sdk.processor.ProcessorRef;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;


public class Pipeline implements Loggable<Integer> {

    /**
     * The id of the pipeline.  This is an Integer so we can set it to
     * null if a pipeline is exported.
     */
    private Integer id;

    private Boolean versionUp;

    private int version;

    @NotNull
    private PipelineType type;

    @NotEmpty
    @Pattern(regexp="^[a-z].*$", flags={Pattern.Flag.CASE_INSENSITIVE})
    private String name;

    private String description;

    private boolean standard;

    @NotNull
    private List<ProcessorRef> processors;

    public Integer getId() {
        return id;
    }

    public Pipeline setId(Integer id) {
        this.id = id;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public Pipeline setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Pipeline setName(String name) {
        this.name = name;
        return this;
    }

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public Pipeline setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public Pipeline setVersion(int version) {
        this.version = version;
        return this;
    }

    public Boolean getVersionUp() {
        return versionUp;
    }

    public Pipeline setVersionUp(Boolean versionUp) {
        this.versionUp = versionUp;
        return this;
    }

    @Override
    public Integer getTargetId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pipeline pipeline = (Pipeline) o;
        return getId().equals(pipeline.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("name", name)
                .toString();
    }

    public String getDescription() {
        return description;
    }

    public Pipeline setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isStandard() {
        return standard;
    }

    public Pipeline setStandard(boolean standard) {
        this.standard = standard;
        return this;
    }
}
