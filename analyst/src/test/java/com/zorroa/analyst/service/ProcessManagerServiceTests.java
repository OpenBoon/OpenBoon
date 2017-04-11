package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.analyst.AnalystProcess;
import com.zorroa.common.cluster.ClusterException;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.TaskState;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        ExecuteTaskStart ts = new ExecuteTaskStart(task, 0, 0)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setName("task")
                .setSharedPath("../unittest/shared")
                .setArchivistHost("http://localhost:8066")
                .setRootPath(Files.createTempDirectory("zorroa_analyst_test").toString());

        File dir = new File(ts.getScriptPath()).getParentFile();
        dir.mkdirs();
        File logs = new File(ts.getLogPath()).getParentFile();
        logs.mkdirs();

        Files.copy(resources.resolve("scripts/import.zps"), Paths.get(ts.getScriptPath()));
        Future<AnalystProcess> proc = processManager.execute(ts, false);
        assertEquals(TaskState.Success, proc.get().getNewState());
    }

    @Test(expected = ClusterException.class)
    public void testExecuteSameTask() throws IOException, ExecutionException, InterruptedException {
        ExecuteTaskStart ts = new ExecuteTaskStart(task, 0, 0)
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setName("task")
                .setSharedPath("../unittest/shared")
                .setArchivistHost("http://localhost:8066")
                .setRootPath(Files.createTempDirectory("zorroa_analyst_test").toString());

        File dir = new File(ts.getScriptPath()).getParentFile();
        dir.mkdirs();

        Files.copy(resources.resolve("scripts/import.zps"), Paths.get(ts.getScriptPath()));

        processManager.execute(ts, true);
        Future<AnalystProcess> proc2 = processManager.execute(ts,false);

        assertNull(proc2);
    }
}
