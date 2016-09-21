package com.zorroa.common.domain;

/**
 * Created by chambers on 9/15/16.
 */
public class ExecuteTaskStats {

    private ExecuteTask task;

    private int warningCount = 0;
    private int errorCount = 0;
    private int successCount = 0;

    public ExecuteTaskStats() { }

    public ExecuteTaskStats(ExecuteTask task) {
        this.task = task;
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

    public ExecuteTask getTask() {
        return task;
    }

    public ExecuteTaskStats setTask(ExecuteTask task) {
        this.task = task;
        return this;
    }
}
