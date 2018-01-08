package com.zorroa.analyst.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by chambers on 3/13/17.
 */
@Component
public class ShutdownHook implements ApplicationListener<ContextClosedEvent> {

    @Autowired
    ProcessManagerService processManagerService;

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        processManagerService.killAllTasks();
    }

}
