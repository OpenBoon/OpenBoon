package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.common.domain.TaskId;
import com.zorroa.common.domain.TaskState;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Created by chambers on 7/13/16.
 */
public class Task implements TaskId {

    public static final int ORDER_DEFAULT = 10;
    public static final int ORDER_INTERACTIVE = -1;

    private Integer taskId;
    private Integer parentId;
    private Integer jobId;
    private String name;
    private String host;
    private TaskState state;
    private long timeStarted;
    private long timeStopped;
    private long timeCreated;
    private long timeStateChange;
    private int exitStatus;
    private int order;

    private Stats stats;

    public int getId() {
        return taskId;
    }

    @Override
    public Integer getTaskId() {
        return taskId;
    }

    @Override
    public Integer getParentTaskId() {
        return parentId;
    }

    @Override
    public Integer getJobId() {
        return jobId;
    }

    public String getName() {
        return name;
    }

    public Task setName(String name) {
        this.name = name;
        return this;
    }

    public Task setTaskId(Integer taskId) {
        this.taskId = taskId;
        return this;
    }

    public Task setJobId(Integer jobId) {
        this.jobId = jobId;
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

    public Integer getParentId() {
        return parentId;
    }

    public Task setParentId(Integer parentId) {
        this.parentId = parentId;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public Task setOrder(int order) {
        this.order = order;
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

    public Stats getStats() {
        return stats;
    }

    public Task setStats(Stats stats) {
        this.stats = stats;
        return this;
    }

    public static class Stats {
        private int frameSuccessCount;
        private int frameErrorCount;
        private int frameWarningCount;
        private int frameTotalCount;

        public int getFrameSuccessCount() {
            return frameSuccessCount;
        }

        public Stats setFrameSuccessCount(int frameSuccessCount) {
            this.frameSuccessCount = frameSuccessCount;
            return this;
        }

        public int getFrameErrorCount() {
            return frameErrorCount;
        }

        public Stats setFrameErrorCount(int frameErrorCount) {
            this.frameErrorCount = frameErrorCount;
            return this;
        }

        public int getFrameWarningCount() {
            return frameWarningCount;
        }

        public Stats setFrameWarningCount(int frameWarningCount) {
            this.frameWarningCount = frameWarningCount;
            return this;
        }

        public int getFrameTotalCount() {
            return frameTotalCount;
        }

        public Stats setFrameTotalCount(int frameTotalCount) {
            this.frameTotalCount = frameTotalCount;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("taskId", taskId)
                .add("jobId", jobId)
                .add("name", name)
                .toString();
    }
}
