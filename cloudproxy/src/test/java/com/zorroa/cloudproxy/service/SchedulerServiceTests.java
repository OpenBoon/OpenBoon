package com.zorroa.cloudproxy.service;

import com.google.common.collect.Maps;
import com.zorroa.cloudproxy.AbstractTest;
import com.zorroa.cloudproxy.domain.ImportTask;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by chambers on 3/29/17.
 */
public class SchedulerServiceTests extends AbstractTest {

    @Autowired
    SchedulerService schedulerService;

    @Test
    public void testGetNextRuntime() {
        Date nextRunTime = schedulerService.getNextRunTime();
        assertTrue(nextRunTime.toString().startsWith("Tue Dec 12 12:12:00"));
    }

    @Test
    public void testReloadAndRestart() {
        schedulerService.reloadAndRestart(false);
    }

    @Test
    public void testImportTaskEnvironment() throws IOException {

        Map<String, String> current = Maps.newHashMap();
        Map<String, String> expected = Maps.newHashMap();
        expected.put("ZORROA_HMAC_KEY", "100-100-100-100");
        expected.put("ZORROA_ARCHIVIST_URL","https://nohost.com");
        expected.put("ZORROA_USER", "remote_user");

        ImportTask task = null;
        try {
            task = schedulerService.startImportTask(false);
            List<String> lines = Files.readAllLines(task.getWorkDir().resolve("env"));
            for (String l: lines) {
                String[] e = l.split("=", 2);
                current.put(e[0], e[1]);
            }
            for (Map.Entry<String,String> e: expected.entrySet()) {
                logger.info("Checking {} env value", e.getKey());
                assertNotNull(current.get(e.getKey()));
                assertEquals(e.getValue(), current.get(e.getKey()));
            }

        } finally {
            if (task != null) {
                schedulerService.cleanupImportTaskWorkDir(task);
            }
        }
    }
}
