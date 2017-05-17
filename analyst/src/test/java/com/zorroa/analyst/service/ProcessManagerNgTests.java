package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

/**
 * Created by chambers on 5/8/17.
 */
public class ProcessManagerNgTests extends AbstractTest {

    @Autowired
    ProcessManagerNgService processManagerNg;

    int task = 0;

    @Test
    public void testExecute() throws IOException, ExecutionException, InterruptedException {

        TaskStartT ts = new TaskStartT()
                .setId(1)
                .setJobId(1)
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
