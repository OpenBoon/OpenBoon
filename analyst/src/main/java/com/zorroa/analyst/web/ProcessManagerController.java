package com.zorroa.analyst.web;

import com.zorroa.analyst.service.ProcessManagerService;
import com.zorroa.common.domain.ExecuteTaskStart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 2/16/16.
 */
@RestController
public class ProcessManagerController {

    @Autowired
    ProcessManagerService processManager;

    @RequestMapping(value="/api/v1/task/_execute", method=RequestMethod.POST)
    public void executeScript(@RequestBody ExecuteTaskStart task) throws Throwable {
        processManager.queueExecute(task);
    }
}
