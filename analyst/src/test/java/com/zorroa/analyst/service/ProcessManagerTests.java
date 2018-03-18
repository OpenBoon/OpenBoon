package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.cluster.thrift.TaskStartT;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Created by chambers on 5/8/17.
 */
public class ProcessManagerTests extends AbstractTest {

    @Autowired
    ProcessManagerService processManager;

    int task = 0;

    UUID id = UUID.fromString("1FA96F09-E782-4041-9117-DEBA4BEFA924");

    @Test
    public void testExecute() throws IOException, ExecutionException, InterruptedException {

        TaskStartT ts = new TaskStartT()
                .setId(id.toString())
                .setJobId(id.toString())
                .setArgMap(Json.serialize(ImmutableMap.of("path", "../unittest/resources/images/set01")))
                .setName("task")
                .setEnv(Maps.newHashMap())
                .setSharedDir("../unittest/shared")
                .setMasterHost("http://localhost:8066")
                .setWorkDir(Files.createTempDirectory("zorroa_analyst_test").toString());

        ts.setScriptPath(resources
                .resolve("scripts/import.zps").toString());
        //ClusterProcess p = processManagerNg.executeClusterTask(ts);
        //assertEquals(0, p.getExitStatus());
    }

}
