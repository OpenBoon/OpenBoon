package com.zorroa.archivist.domain;

import com.zorroa.common.cluster.thrift.TaskStatsT;
import com.zorroa.sdk.domain.AssetIndexResult;

public class TaskStatsAdder {

    public int create = 0;
    public int update = 0;
    public int warning = 0;
    public int replace = 0;
    public int error = 0;
    public int total = 0;

    public TaskStatsAdder() { }

    /**
     * Only thrift sets success/failure.
     *
     * @param stats
     */
    public TaskStatsAdder(TaskStatsT stats) {
        error = stats.errorCount;
        warning = stats.warningCount;
    }

    public TaskStatsAdder(AssetIndexResult result) {
        error = result.errors;
        update = result.updated;
        replace = result.replaced;
        warning = result.warnings;
        create = result.created;
    }

    public int getCreate() {
        return create;
    }

    public TaskStatsAdder setCreate(int create) {
        this.create = create;
        return this;
    }

    public int getUpdate() {
        return update;
    }

    public TaskStatsAdder setUpdate(int update) {
        this.update = update;
        return this;
    }

    public int getWarning() {
        return warning;
    }

    public TaskStatsAdder setWarning(int warning) {
        this.warning = warning;
        return this;
    }

    public int getReplace() {
        return replace;
    }

    public TaskStatsAdder setReplace(int replace) {
        this.replace = replace;
        return this;
    }

    public int getError() {
        return error;
    }

    public TaskStatsAdder setError(int error) {
        this.error = error;
        return this;
    }

    public int getTotal() {
        return total;
    }

    public TaskStatsAdder setTotal(int total) {
        this.total = total;
        return this;
    }
}
