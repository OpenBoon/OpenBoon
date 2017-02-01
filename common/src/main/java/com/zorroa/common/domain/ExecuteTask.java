package com.zorroa.common.domain;

/**
 * Created by chambers on 8/23/16.
 */
public class ExecuteTask implements TaskId, JobId {

    public static final String scriptPath(String root, String name, int id) {
        return String.format("%s/scripts/%s.%04d.json",
                root, name.replaceAll("[\\s\\.\\/\\\\]+", "_"), id);
    }

    public static final String logPath(String root, String name, int id) {
        return String.format("%s/logs/%s.%04d.log",
                root, name.replaceAll("[\\s\\.\\/\\\\]+", "_"), id);
    }

    private Integer taskId;
    private Integer jobId;
    private Integer parentTaskId;

    public ExecuteTask() { }

    public ExecuteTask(int jobId, int taskId) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.parentTaskId = null;
    }

    public ExecuteTask(int jobId, int taskId, int parentTaskId) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.parentTaskId = parentTaskId;
    }

    public ExecuteTask(ExecuteTask task) {
        this.taskId = task.getTaskId();
        this.jobId = task.getJobId();
        this.parentTaskId = task.getParentTaskId();
    }

    public Integer getTaskId() {
        return taskId;
    }

    public ExecuteTask setTaskId(Integer taskId) {
        this.taskId = taskId;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ExecuteTask setJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public ExecuteTask setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
        return this;
    }
}
