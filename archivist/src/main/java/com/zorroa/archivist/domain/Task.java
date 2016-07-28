package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.sdk.zps.ZpsTask;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Created by chambers on 7/13/16.
 */
public class Task implements ZpsTask {

    private Integer taskId;
    private Integer parentId;
    private Integer jobId;
    private String execute;
    private String host;
    private TaskState state;
    private long timeStarted;
    private long timeStopped;
    private long timeCreated;
    private long timeStateChange;
    private int exitStatus;
    private String script;

    @Override
    public Integer getTaskId() {
        return taskId;
    }

    @Override
    public Integer getJobId() {
        return jobId;
    }

    public Task setTaskId(Integer taskId) {
        this.taskId = taskId;
        return this;
    }

    public Task setJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    public String getExecute() {
        return execute;
    }

    public Task setExecute(String execute) {
        this.execute = execute;
        return this;
    }

    public String getHost() {
        return host;
    }

    public Task setHost(String host) {
        this.host = host;
        return this;
    }

    public TaskState getState() {
        return state;
    }

    public Task setState(TaskState state) {
        this.state = state;
        return this;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public Task setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
        return this;
    }

    public long getTimeStopped() {
        return timeStopped;
    }

    public Task setTimeStopped(long timeStopped) {
        this.timeStopped = timeStopped;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public Task setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public long getTimeStateChange() {
        return timeStateChange;
    }

    public Task setTimeStateChange(long timeStateChange) {
        this.timeStateChange = timeStateChange;
        return this;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public Task setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
        return this;
    }

    public String getScript() {
        return script;
    }

    public Task setScript(String script) {
        this.script = script;
        return this;
    }

    public Integer getParentId() {
        return parentId;
    }

    public Task setParentId(Integer parentId) {
        this.parentId = parentId;
        return this;
    }

    public String duration() {
        if (timeStarted <= 0) {
            return "00:00:00";
        }
        else {
            long stopped = timeStopped <= 0 ? System.currentTimeMillis(): timeStopped;
            return DurationFormatUtils.formatDuration(
                    stopped - timeStarted, "HH:mm:ss", true);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("taskId", taskId)
                .add("jobId", jobId)
                .add("execute", execute)
                .toString();
    }
}
