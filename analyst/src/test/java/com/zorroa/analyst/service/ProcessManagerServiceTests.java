package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * Note: we currently only create the mapping in Archivist.
 */
public class ProcessManagerServiceTests extends AbstractTest {

    @Autowired
    ProcessManagerService processManager;

    @Test
    public void testExecute() throws IOException {
        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/import.zps"));
        processManager.execute(new ExecuteTaskStart()
                .setArgs(ImmutableMap.of("path", "../unittest/resources/images/set01"))
                .setScript(Json.serializeToString(zps)));
    }

}
