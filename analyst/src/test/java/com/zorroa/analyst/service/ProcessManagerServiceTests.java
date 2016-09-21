package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.analyst.AnalystProcess;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStop;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Note: we currently only create the mapping in Archivist.
 */
public class ProcessManagerServiceTests extends AbstractTest {

    @Autowired
    ProcessManagerService processManager;

    int task = 0;

    @Before
    public void init() {
        task++;
    }

    @Test
    public void testExecute() throws IOException, ExecutionException, InterruptedException {
        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/import.zps"));
        Future<AnalystProcess> proc = processManager.execute(new ExecuteTaskStart(task, 0, 0)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setScript(Json.serializeToString(zps))
                .setLogPath(Files.createTempFile("analyst", "test").toString()), false);
        assertEquals(TaskState.Success, proc.get().getNewState());
    }

    @Test
    public void testExecuteSameTask() throws IOException, ExecutionException, InterruptedException {
        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/sleep.zps"));
        processManager.execute(new ExecuteTaskStart(task, 3, 3)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setScript(Json.serializeToString(zps))
                .setLogPath(Files.createTempFile("analyst", "test").toString()), true);

        Future<AnalystProcess> proc2 = processManager.execute(new ExecuteTaskStart(task, 3, 3)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setScript(Json.serializeToString(zps))
                .setLogPath(Files.createTempFile("analyst", "test").toString()), false);

        assertNull(proc2);
    }

    @Test
    public void testStop() throws IOException, InterruptedException {

        /**
         * Execute a sleep task.
         */
        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/sleep.zps"));
        processManager.execute(new ExecuteTaskStart(1, 1, 1)
                .setScript(Json.serializeToString(zps))
                .setLogPath(Files.createTempFile("analyst", "test").toString()), true);

        Thread.sleep(1000);
        assertTrue(processManager.stopTask(new ExecuteTaskStop(task, 1, 1).setReason("manual kill")));
    }

    @Test
    public void testStopFailure() throws IOException, InterruptedException {
        assertFalse(processManager.stopTask(new ExecuteTaskStop(task, 2, 2).setReason("manual kill")));
    }
}
