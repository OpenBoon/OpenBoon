package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.common.domain.EventLoggable;
import com.zorroa.sdk.plugins.ModuleRef;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;


public class Pipeline implements EventLoggable {
    private int id;

    @NotNull
    private PipelineType type;

    @NotEmpty
    @Pattern(regexp="^[a-z].*$", flags={Pattern.Flag.CASE_INSENSITIVE})
    private String name;

    @NotNull
    private String description;

    @NotNull
    private List<ModuleRef> processors;

    public int getId() {
        return id;
    }

    public Pipeline setId(int id) {
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

    public List<ModuleRef> getProcessors() {
        return processors;
    }

    public Pipeline setProcessors(List<ModuleRef> processors) {
        this.processors = processors;
        return this;
    }

    @Override
    public String getEventLogId() {
        return String.valueOf(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pipeline pipeline = (Pipeline) o;
        return getId() == pipeline.getId();
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
}
