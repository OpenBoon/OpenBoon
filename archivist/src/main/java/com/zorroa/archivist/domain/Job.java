package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.zps.ZpsJob;

import java.util.Map;
import java.util.Objects;

/**
 * Created by chambers on 7/12/16.
 */
public class Job implements ZpsJob {

    private int id;
    private String name;
    private PipelineType type;
    private String userCreated;
    private JobState state;

    private long timeStarted;
    private long timeStopped;

    private Counts counts;
    private Stats stats;

    public int getId() {
        return id;
    }

    public Job setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Job setName(String name) {
        this.name = name;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public Job setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public String getUserCreated() {
        return userCreated;
    }

    public Job setUserCreated(String userCreated) {
        this.userCreated = userCreated;
        return this;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public Job setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
        return this;
    }

    public long getTimeStopped() {
        return timeStopped;
    }

    public Job setTimeStopped(long timeStopped) {
        this.timeStopped = timeStopped;
        return this;
    }

    public Counts getCounts() {
        return counts;
    }

    public Job setCounts(Counts counts) {
        this.counts = counts;
        return this;
    }

    public Stats getStats() {
        return stats;
    }

    public Job setStats(Stats stats) {
        this.stats = stats;
        return this;
    }

    public JobState getState() {
        return state;
    }

    public Job setState(JobState state) {
        this.state = state;
        return this;
    }

    public Map<String,Float> getProgress() {
        if (counts.getTasksTotal() == 0) {
            return ImmutableMap.of(
                    "running", 0f,
                    "success", 0f,
                    "waiting", 0f,
                    "failed", 0f);
        }
        else {
            float t = (float) counts.getTasksTotal();
            return ImmutableMap.of(
                    "running", (counts.getTasksRunning() / t) * 100,
                    "success", (counts.getTasksSuccess() / t) * 100,
                    "failed", (counts.getTasksFailure() / t) * 100,
                    "waiting", ((counts.getTasksWaiting() + counts.getTasksQueued()) / t) * 100);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return getId() == job.getId();
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
                .add("type", type)
                .toString();
    }

    @Override
    public Integer getJobId() {
        return id;
    }

    /**
     * Counts related to the job's tasks.
     */
    public static class Counts {
        private int tasksTotal;
        private int tasksCompleted;
        private int tasksWaiting;
        private int tasksQueued;
        private int tasksRunning;
        private int tasksSuccess;
        private int tasksFailure;

        public int getTasksCompleted() {
            return tasksCompleted;
        }

        public Counts setTasksCompleted(int tasksCompleted) {
            this.tasksCompleted = tasksCompleted;
            return this;
        }

        public int getTasksTotal() {
            return tasksTotal;
        }

        public Counts setTasksTotal(int tasksTotal) {
            this.tasksTotal = tasksTotal;
            return this;
        }

        public int getTasksWaiting() {
            return tasksWaiting;
        }

        public Counts setTasksWaiting(int tasksWaiting) {
            this.tasksWaiting = tasksWaiting;
            return this;
        }

        public int getTasksQueued() {
            return tasksQueued;
        }

        public Counts setTasksQueued(int tasksQueued) {
            this.tasksQueued = tasksQueued;
            return this;
        }

        public int getTasksRunning() {
            return tasksRunning;
        }

        public Counts setTasksRunning(int tasksRunning) {
            this.tasksRunning = tasksRunning;
            return this;
        }

        public int getTasksSuccess() {
            return tasksSuccess;
        }

        public Counts setTasksSuccess(int tasksSuccess) {
            this.tasksSuccess = tasksSuccess;
            return this;
        }

        public int getTasksFailure() {
            return tasksFailure;
        }

        public Counts setTasksFailure(int tasksFailure) {
            this.tasksFailure = tasksFailure;
            return this;
        }
    }

    /**
     * Counts and data related to the jobs output.
     */
    public static class Stats {
        private int assetTotal;
        private int assetCreated;
        private int assetUpdated;
        private int assetErrored;
        private int assetWarning;

        public int getAssetTotal() {
            return assetTotal;
        }

        public Stats setAssetTotal(int assetTotal) {
            this.assetTotal = assetTotal;
            return this;
        }

        public int getAssetCreated() {
            return assetCreated;
        }

        public Stats setAssetCreated(int assetCreated) {
            this.assetCreated = assetCreated;
            return this;
        }

        public int getAssetUpdated() {
            return assetUpdated;
        }

        public Stats setAssetUpdated(int assetUpdated) {
            this.assetUpdated = assetUpdated;
            return this;
        }

        public int getAssetErrored() {
            return assetErrored;
        }

        public Stats setAssetErrored(int assetErrored) {
            this.assetErrored = assetErrored;
            return this;
        }

        public int getAssetWarning() {
            return assetWarning;
        }

        public Stats setAssetWarning(int assetWarning) {
            this.assetWarning = assetWarning;
            return this;
        }
    }
}
