package com.zorroa.analyst.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class ShutdownHook  @Autowired constructor(
        private val processManagerService: ProcessManagerService) : ApplicationListener<ContextClosedEvent> {

    override fun onApplicationEvent(contextClosedEvent: ContextClosedEvent) {
        processManagerService.killAllTasks()
    }

}
