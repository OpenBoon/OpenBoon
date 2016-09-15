package com.zorroa.common.domain;

/**
 * Created by chambers on 9/15/16.
 */
public class ExecuteTaskStats extends ExecuteTask {

    private int warningCount = 0;
    private int errorCount = 0;
    private int successCount = 0;

    public ExecuteTaskStats() { }

    public ExecuteTaskStats(ExecuteTask task) {
        this.setJobId(task.getJobId());
        this.setTaskId(task.getTaskId());
        this.setParentTaskId(task.getParentTaskId());
    }

    public int getSuccessCount() {
        return successCount;
    }

    public ExecuteTaskStats setSuccessCount(int successCount) {
        this.successCount = successCount;
        return this;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public ExecuteTaskStats setWarningCount(int warningCount) {
        this.warningCount = warningCount;
        return this;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public ExecuteTaskStats setErrorCount(int errorCount) {
        this.errorCount = errorCount;
        return this;
    }
}
