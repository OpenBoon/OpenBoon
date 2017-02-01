package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.analyst.AnalystProcess;
import com.zorroa.common.cluster.ClusterException;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStop;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.util.FileUtils;
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

    public String jobRootPath;
    public String absoluteShared;

    @Before
    public void init() {
        task++;
        jobRootPath = FileUtils.normalize("../zorroa-test-data");
        logger.info("{}", jobRootPath);
    }

    @Test
    public void testExecute() throws IOException, ExecutionException, InterruptedException {
        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/import.zps"));
        Future<AnalystProcess> proc = processManager.execute(new ExecuteTaskStart(task, 0, 0)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setRootPath(Files.createTempFile("analyst", "test").toString()), false);
        assertEquals(TaskState.Success, proc.get().getNewState());
    }

    @Test(expected = ClusterException.class)
    public void testExecuteSameTask() throws IOException, ExecutionException, InterruptedException {
        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/sleep.zps"));
        processManager.execute(new ExecuteTaskStart(task, 3, 3)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setRootPath(Files.createTempFile("analyst", "test").toString()), true);

        Future<AnalystProcess> proc2 = processManager.execute(new ExecuteTaskStart(task, 3, 3)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setRootPath(Files.createTempFile("analyst", "test").toString()), false);

        assertNull(proc2);
    }

    /**
     * This test requires core plugins to be imstalled .
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testStop() throws IOException, InterruptedException {
        /*
        Future<AnalystProcess> p = processManager.execute(new ExecuteTaskStart(1, 1, 1)
                .setScript(Json.serializeToString(new ZpsScript()
                        .setOver(Lists.newArrayList(new Document()))
                        .setExecute(ImmutableList.of(
                                new ProcessorRef("com.zorroa.core.processor.GroovyScript")
                                        .setArg("script", "sleep(10000);")))))
                .setRootPath(Files.createTempFile("analyst", "test").toString()), true);

        Thread.sleep(2000);
        assertTrue(processManager.stopTask(new ExecuteTaskStop(task, 1, 1).setReason("manual kill")));
        */
    }

    @Test
    public void testStopFailure() throws IOException, InterruptedException {
        assertFalse(processManager.stopTask(new ExecuteTaskStop(task, 2, 2).setReason("manual kill")));
    }
}
