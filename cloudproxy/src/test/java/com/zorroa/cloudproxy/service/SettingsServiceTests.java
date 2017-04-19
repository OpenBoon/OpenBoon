package com.zorroa.cloudproxy.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.cloudproxy.AbstractTest;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.cloudproxy.domain.ImportStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/28/17.
 */
public class SettingsServiceTests extends AbstractTest {

    @Autowired
    SettingsService settingsService;

    @Value("${cloudproxy.paths.config}")
    private String configPath;

    @Test
    public void getImportStats() {
        ImportStatus last = settingsService.getImportStats();
        assertEquals(null, last.getStartTime());
        assertEquals(null, last.getFinishTime());
    }

    @Test
    public void testGetSettings() {
        Settings config = settingsService.getSettings();
        assertNotNull(config);
        assertEquals("https://nohost.com", config.getArchivistUrl());
        assertEquals("100-100-100-100", config.getHmacKey());
        assertEquals(ImmutableList.of("/foo"), config.getPaths());
        assertEquals("0 12 12 12 12 ?", config.getSchedule());
        assertEquals(4, config.getThreads());
        assertEquals(22, (int) config.getPipelineId());
    }

    @Test
    public void testSaveSettings() throws InterruptedException, IOException {
        File configFile = new File(configPath  + "/config.json");
        long modifiedTime = configFile.lastModified();
        Thread.sleep(5);

        Settings config = settingsService.getSettings();
        config.setArchivistUrl("https://nohost.com");
        config.setHmacKey("100-100-100-100");
        config.setPaths(ImmutableList.of("/foo"));
        config.setPipelineId(22);
        config.setSchedule("0 12 12 12 12 ?");
        config.setThreads(4);

        settingsService.saveSettings(config);
        assertTrue(configFile.lastModified() > modifiedTime);
    }
}
