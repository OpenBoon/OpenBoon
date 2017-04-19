package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.domain.ImportTask;

import java.util.concurrent.Future;

/**
 * Created by chambers on 4/19/17.
 */
public interface ImportTaskService {
    Future<ImportTask> submitImportTask(boolean cleanup);

    ImportTask runImportTask(boolean cleanup);

    void cancelAllTasks();

    boolean cancelRunningImportTask();

    ImportTask getActiveImportTask();

    boolean isImportTaskRunning();
}
