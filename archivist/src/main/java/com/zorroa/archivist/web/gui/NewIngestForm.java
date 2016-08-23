package com.zorroa.archivist.web.gui;

import com.zorroa.archivist.domain.Schedule;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * Created by chambers on 7/10/16.
 */
public class NewIngestForm {

    /**
     * An optional folderId for aggregating the on-disk filesystem.
     */
    private Integer folderId;

    /**
     * A Pipeline to utilize.  Ingests cannot do ad-hoc pipelines.
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
     * Ingests should run automatically on given schedule.
     */
    private boolean automatic = true;

    /**
     * Create and Import job immediately.
     */
    private boolean runNow = true;

    /**
     * Paths to ingest
     */
    @NotEmpty
    private List<String> paths;

    public Integer getFolderId() {
        return folderId;
    }

    public NewIngestForm setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public NewIngestForm setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public String getName() {
        return name;
    }

    public NewIngestForm setName(String name) {
        this.name = name;
        return this;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public NewIngestForm setSchedule(Schedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public NewIngestForm setAutomatic(boolean automatic) {
        this.automatic = automatic;
        return this;
    }

    public boolean isRunNow() {
        return runNow;
    }

    public NewIngestForm setRunNow(boolean runNow) {
        this.runNow = runNow;
        return this;
    }

    public List<String> getPaths() {
        return paths;
    }

    public NewIngestForm setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }
}
