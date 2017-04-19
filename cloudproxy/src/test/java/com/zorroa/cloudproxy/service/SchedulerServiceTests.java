package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.AbstractTest;
import com.zorroa.cloudproxy.domain.Settings;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/29/17.
 */
public class SchedulerServiceTests extends AbstractTest {

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    SettingsService settingsService;

    @Test
    public void testGetNextRuntime() throws IOException {
        Settings settings = settingsService.getSettings();
        settings.setSchedule("0 12 12 12 12 ?");
        settingsService.saveSettings(settings);

        schedulerService.reloadAndRestart(false);

        Date nextRunTime = schedulerService.getNextRunTime();
        assertTrue(nextRunTime.toString().startsWith("Tue Dec 12 12:12:00"));
    }

    @Test
    public void testReloadAndRestart() {
        schedulerService.reloadAndRestart(false);
    }

}
