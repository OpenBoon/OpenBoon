package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
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

        ZpsScript zps = ZpsScript.load(new File("../unittest/resources/scripts/image.zps"));
        processManager.execute(zps, ImmutableMap.of("path", "../unittest/resources/images/set01"));

    }

}
