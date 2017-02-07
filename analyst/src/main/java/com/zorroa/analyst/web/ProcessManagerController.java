package com.zorroa.analyst.web;

import com.zorroa.analyst.AnalystProcess;
import com.zorroa.analyst.service.ProcessManagerService;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * All methods here are async. The calling thread gets no indication of
 * what actually happened to avoid blocking on the archivist.
 */
@RestController
public class ProcessManagerController {

    @Autowired
    ProcessManagerService processManager;

    @RequestMapping(value="/api/v1/task/_execute", method=RequestMethod.POST)
    public void executeTask(@RequestBody ExecuteTaskStart task) throws Throwable {
        processManager.execute(task, true);
    }

    @RequestMapping(value="/api/v1/task/_stop", method=RequestMethod.POST)
    public void stopTask(@RequestBody ExecuteTaskStop task) throws Throwable {
        processManager.asyncStopTask(task);
    }

    @RequestMapping(value="/api/v1/task", method=RequestMethod.GET)
    public Collection<AnalystProcess> tasks() throws Throwable {
        return processManager.getProcesses();
    }
}
