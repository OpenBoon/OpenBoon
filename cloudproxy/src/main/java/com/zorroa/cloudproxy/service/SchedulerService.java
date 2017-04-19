package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.domain.ImportTask;

import java.io.FileNotFoundException;
import java.util.Date;

/**
 * Created by chambers on 3/24/17.
 */
public interface SchedulerService {

    Date getNextRunTime();

    void reloadAndRestart(boolean allowStartNow);

    /**
     * Start a task in the foreground.
     */
    ImportTask executeImportTask(boolean cleanup);

    void cleanupImportTaskWorkDir(ImportTask task) throws FileNotFoundException;
}
