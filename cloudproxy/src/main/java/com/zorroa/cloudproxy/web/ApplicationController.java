package com.zorroa.cloudproxy.web;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.cloudproxy.domain.FilesystemEntry;
import com.zorroa.cloudproxy.domain.ImportStatus;
import com.zorroa.cloudproxy.domain.ImportTask;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.cloudproxy.service.FilesystemService;
import com.zorroa.cloudproxy.service.ImportTaskService;
import com.zorroa.cloudproxy.service.SchedulerService;
import com.zorroa.cloudproxy.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 3/28/17.
 */
@RestController
@CrossOrigin(origins = "http://localhost:8080")
public class ApplicationController {

    @Autowired
    SettingsService configService;

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    FilesystemService filesystemService;

    @Autowired
    ImportTaskService importTaskService;

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.GET)
    public Settings get()    {
        return configService.getSettings();
    }

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.PUT)
    public Settings update(@RequestBody Settings props) throws IOException {
        Settings newSettings = configService.saveSettings(props);
        schedulerService.reloadAndRestart(true);
        return newSettings;
    }

    @Deprecated
    @RequestMapping(value="/api/v1/stats", method= RequestMethod.GET)
    public ImportStatus stats() throws IOException {
        return importStats();
    }

    @RequestMapping(value="/api/v1/import", method= RequestMethod.GET)
    public ImportStatus importStats() throws IOException {
        ImportStatus stats = configService.getImportStats();

        Date nextRunTime = schedulerService.getNextRunTime();
        if (nextRunTime != null) {
            stats.setNextTime(nextRunTime.getTime());
        }

        ImportTask task = importTaskService.getActiveImportTask();
        if (task!=null) {
            stats.setProgress(task.getProgress());
        }

        return stats;
    }

    @RequestMapping(value="/api/v1/import", method= RequestMethod.DELETE)
    public Object cancel() {
        boolean status = importTaskService.cancelRunningImportTask();
        return ImmutableMap.of("uri", "/api/v1/import", "method", "DELETE", "status", status);
    }

    @RequestMapping(value="/api/v1/files/_path", method = RequestMethod.PUT)
    public List<FilesystemEntry> get(@RequestBody Map<String,String> path) {
        return filesystemService.get(path.get("path"), Lists.newArrayList("/etc", "/usr/bin", "/usr/lib"));
    }
}
