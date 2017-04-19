package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.AbstractTest;
import com.zorroa.cloudproxy.domain.ImportTask;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Future;

import static org.junit.Assert.assertFalse;

/**
 * Created by chambers on 4/19/17.
 */
public class ImportTaskServiceTests extends AbstractTest {

    @Autowired
    ImportTaskService importTaskService;

    @Test
    public void submitImportTask() throws Exception {
        Future<ImportTask> tasks = importTaskService.submitImportTask(true);
        tasks.get();
    }

    @Test
    public void cancelRunningImportTask() throws Exception {
        importTaskService.submitImportTask(true);
        Thread.sleep(2000);
        importTaskService.cancelRunningImportTask();
        Thread.sleep(2000);
        assertFalse(importTaskService.isImportTaskRunning());
    }

    @Test
    public void cancelAllTasks() throws Exception {
        Future<ImportTask> tasks = importTaskService.submitImportTask(true);
        Thread.sleep(2000);
        importTaskService.cancelAllTasks();
        Thread.sleep(2000);
        assertFalse(importTaskService.isImportTaskRunning());
    }
}
