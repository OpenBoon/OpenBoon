package com.zorroa.archivist.domain;

import com.zorroa.common.cluster.thrift.TaskStatsT;
import com.zorroa.sdk.domain.AssetIndexResult;

public class TaskStatsAdder {

    public int create = 0;
    public int update = 0;
    public int warning = 0;
    public int replace = 0;
    public int error = 0;

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


}
