package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.zorroa.common.domain.JobId;

import java.util.Map;
import java.util.Objects;

/**
 * Created by chambers on 7/12/16.
 */
public class Job implements JobId {

    private int id;
    private String name;
    private PipelineType type;
    private UserBase user;
    private JobState state;
    private Map<String, Object> args;

    private long timeStarted;
    private long timeUpdated;

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

    public UserBase getUser() {
        return user;
    }

    public Job setUser(UserBase user) {
        this.user = user;
        return this;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public Job setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
        return this;
    }

    public long getTimeUpdated() {
        return timeUpdated;
    }

    public Job setTimeUpdated(long timeUpdated) {
        this.timeUpdated = timeUpdated;
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

    public Map<String, Object> getArgs() {
        return args;
    }

    public Job setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    Map<String, Float> NO_PROGRESS = ImmutableMap.<String, Float>builder()
            .put("running", 0f)
            .put("success", 0f)
            .put("waiting", 0f)
            .put("failed", 0f)
            .put("skipped", 0f)
            .put("total", 0f)
            .build();

    public Map<String,Float> getProgress() {
        if (counts.getTasksTotal() == 0) {
            return NO_PROGRESS;
        }
        else {
            float t = (float) counts.getTasksTotal();
            return ImmutableMap.<String, Float>builder()
                    .put("running", (counts.getTasksRunning() / t) * 100)
                    .put("success", (counts.getTasksSuccess() / t) * 100)
                    .put("waiting", ((counts.getTasksWaiting() + counts.getTasksQueued()) / t) * 100)
                    .put("failed", (counts.getTasksFailure() / t) * 100)
                    .put("skipped", (counts.getTasksSkipped() / t) * 100)
                    .put("total", (counts.getTasksCompleted() / t) * 100)
                    .build();
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
        private int tasksSkipped;

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

        public int getTasksSkipped() {
            return tasksSkipped;
        }

        public Counts setTasksSkipped(int tasksSkipped) {
            this.tasksSkipped = tasksSkipped;
            return this;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("tasksTotal", tasksTotal)
                    .add("tasksCompleted", tasksCompleted)
                    .add("tasksWaiting", tasksWaiting)
                    .add("tasksQueued", tasksQueued)
                    .add("tasksRunning", tasksRunning)
                    .add("tasksSuccess", tasksSuccess)
                    .add("tasksFailure", tasksFailure)
                    .add("tasksSkipped", tasksSkipped)
                    .toString();
        }
    }

    /**
     * Counts and data related to the jobs output.
     */
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
}
