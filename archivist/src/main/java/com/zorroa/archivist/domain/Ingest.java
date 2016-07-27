package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.sdk.domain.EventLoggable;
import com.zorroa.sdk.plugins.ModuleRef;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;

/**
 * Created by chambers on 7/9/16.
 */
public class Ingest implements EventLoggable {

    /**
     * The ID of the ingest.
     */
    private int id;

    /**
     * The date the ingest was created.
     */
    private long timeCreated;

    /**
     * The last time the ingest was executed.
     */
    private long timeExecuted = -1;

    /**
     * The email of the user that created the ingest;
     */
    private int userCreated;

    /**
     * An optional folderId for aggregating the on-disk filesystem.
     */
    private Integer folderId;

    /**
     * An optional pipelineID to utilize.
     */
    private Integer pipelineId;

    /**
     * A handy label for the ingest.
     */
    @NotEmpty
    @Pattern(regexp="^[a-z].*$", flags={Pattern.Flag.CASE_INSENSITIVE})
    private String name;

    /**
     * Import jobs will be created according to this schedule, assuming a job
     * isn't already running for this Ingest.
     */
    @NotNull
    private Schedule schedule;

    /**
     * Ingest should run automatically on given schedule.
     */
    private boolean automatic;

    /**
     * If a pipeline is not set then the Ingest can have its own set of
     * processors.
     */
    private List<ModuleRef> pipeline;

    /**
     * An array of Generator classes for finding files.
     */
    private List<ModuleRef> generators;

    public int getId() {
        return id;
    }

    public Ingest setId(int id) {
        this.id = id;
        return this;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public Ingest setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public Ingest setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Ingest setName(String name) {
        this.name = name;
        return this;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public Ingest setSchedule(Schedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public Ingest setAutomatic(boolean automatic) {
        this.automatic = automatic;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public Ingest setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public long getTimeExecuted() {
        return timeExecuted;
    }

    public Ingest setTimeExecuted(long timeExecuted) {
        this.timeExecuted = timeExecuted;
        return this;
    }

    public int getUserCreated() {
        return userCreated;
    }

    public Ingest setUserCreated(int userCreated) {
        this.userCreated = userCreated;
        return this;
    }

    public List<ModuleRef> getPipeline() {
        return pipeline;
    }

    public Ingest setPipeline(List<ModuleRef> pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public List<ModuleRef> getGenerators() {
        return generators;
    }

    public Ingest setGenerators(List<ModuleRef> generators) {
        this.generators = generators;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingest ingest = (Ingest) o;
        return getId() == ingest.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .toString();
    }

    @Override
    public Object getLogId() {
        return id;
    }

    @Override
    public String getLogType() {
        return "Ingest";
    }
}
