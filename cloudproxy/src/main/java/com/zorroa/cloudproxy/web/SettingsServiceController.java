package com.zorroa.cloudproxy.web;

import com.zorroa.cloudproxy.domain.ImportStatus;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.cloudproxy.service.SchedulerService;
import com.zorroa.cloudproxy.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;

/**
 * Created by chambers on 3/28/17.
 */
@RestController
@CrossOrigin(origins = "http://localhost:8080")
public class SettingsServiceController {

    @Autowired
    SettingsService configService;

    @Autowired
    SchedulerService schedulerService;

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.GET)
    public Settings get() {
        return configService.getSettings();
    }

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.PUT)
    public Settings update(@RequestBody Settings props) throws IOException {
        Settings newSettings = configService.saveSettings(props);
        schedulerService.reloadAndRestart(true);
        return newSettings;
    }

    @RequestMapping(value="/api/v1/stats", method= RequestMethod.GET)
    public ImportStatus stats() throws IOException {
        ImportStatus stats = configService.getImportStats();
        Date nextRunTime = schedulerService.getNextRunTime();
        if (nextRunTime != null) {
            stats.setNextTime(nextRunTime.getTime());
        }
        return stats;
    }

}
