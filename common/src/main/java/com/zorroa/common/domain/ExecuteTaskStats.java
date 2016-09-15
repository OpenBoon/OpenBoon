package com.zorroa.common.domain;

/**
 * Created by chambers on 9/15/16.
 */
public class ExecuteTaskStats extends ExecuteTask {

    private int assetCount = 0;
    private int warningCount = 0;
    private int errorCount = 0;

    public ExecuteTaskStats(ExecuteTask task) {
        this.setJobId(task.getJobId());
        this.setTaskId(task.getTaskId());
        this.setParentTaskId(task.getParentTaskId());
    }

    public int getAssetCount() {
        return assetCount;
    }

    public ExecuteTaskStats setAssetCount(int assetCount) {
        this.assetCount = assetCount;
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
