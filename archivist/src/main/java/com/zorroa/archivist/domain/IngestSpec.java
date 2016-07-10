package com.zorroa.archivist.domain;

/**
 * Created by chambers on 7/9/16.
 */

import com.zorroa.sdk.processor.ProcessorSpec;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

public class IngestSpec {

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
    private Schedule schedule = new Schedule();

    /**
     * Ingest should run automatically on given schedule.
     */
    private boolean automatic = false;

    /**
     * Create and Import job immediately.
     */
    private boolean runNow = false;

    /**
     * If a pipeline is not set then the Ingest can have its own set of
     * processors.
     */
    private List<ProcessorSpec> pipeline;

    /**
     * An array of Generator classes for finding files.
     */
    @NotNull
    private List<ProcessorSpec> generators;

    public Integer getFolderId() {
        return folderId;
    }

    public IngestSpec setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public IngestSpec setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public String getName() {
        return name;
    }

    public IngestSpec setName(String name) {
        this.name = name;
        return this;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public IngestSpec setSchedule(Schedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public IngestSpec setAutomatic(boolean automatic) {
        this.automatic = automatic;
        return this;
    }

    public boolean isRunNow() {
        return runNow;
    }

    public IngestSpec setRunNow(boolean runNow) {
        this.runNow = runNow;
        return this;
    }

    public List<ProcessorSpec> getPipeline() {
        return pipeline;
    }

    public IngestSpec setPipeline(List<ProcessorSpec> pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public List<ProcessorSpec> getGenerators() {
        return generators;
    }

    public IngestSpec setGenerators(List<ProcessorSpec> generators) {
        this.generators = generators;
        return this;
    }
}
